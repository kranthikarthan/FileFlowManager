#!/bin/bash

# File Transfer Microservices - Azure AKS Deployment Script
# This script deploys the file transfer system to Azure Kubernetes Service

set -e

# Configuration
RESOURCE_GROUP=${RESOURCE_GROUP:-"file-transfer-rg"}
AKS_CLUSTER_NAME=${AKS_CLUSTER_NAME:-"file-transfer-aks"}
ACR_NAME=${ACR_NAME:-"filetransferacr"}
LOCATION=${LOCATION:-"East US"}
NAMESPACE=${NAMESPACE:-"file-transfer"}
HELM_RELEASE_NAME=${HELM_RELEASE_NAME:-"file-transfer"}
CHART_PATH=${CHART_PATH:-"./helm/file-transfer"}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Azure CLI is installed
    if ! command -v az &> /dev/null; then
        log_error "Azure CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install it first."
        exit 1
    fi
    
    # Check if Helm is installed
    if ! command -v helm &> /dev/null; then
        log_error "Helm is not installed. Please install it first."
        exit 1
    fi
    
    # Check if user is logged in to Azure
    if ! az account show &> /dev/null; then
        log_error "Not logged in to Azure. Please run 'az login' first."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Create or get AKS cluster
setup_aks_cluster() {
    log_info "Setting up AKS cluster..."
    
    # Create resource group if it doesn't exist
    if ! az group show --name $RESOURCE_GROUP &> /dev/null; then
        log_info "Creating resource group $RESOURCE_GROUP..."
        az group create --name $RESOURCE_GROUP --location "$LOCATION"
        log_success "Resource group created"
    else
        log_info "Resource group $RESOURCE_GROUP already exists"
    fi
    
    # Create AKS cluster if it doesn't exist
    if ! az aks show --resource-group $RESOURCE_GROUP --name $AKS_CLUSTER_NAME &> /dev/null; then
        log_info "Creating AKS cluster $AKS_CLUSTER_NAME..."
        az aks create \
            --resource-group $RESOURCE_GROUP \
            --name $AKS_CLUSTER_NAME \
            --node-count 3 \
            --node-vm-size Standard_D2s_v3 \
            --enable-addons monitoring \
            --enable-managed-identity \
            --enable-workload-identity \
            --enable-oidc-issuer \
            --generate-ssh-keys \
            --location "$LOCATION" \
            --kubernetes-version 1.27.7
        log_success "AKS cluster created"
    else
        log_info "AKS cluster $AKS_CLUSTER_NAME already exists"
    fi
    
    # Get AKS credentials
    log_info "Getting AKS credentials..."
    az aks get-credentials --resource-group $RESOURCE_GROUP --name $AKS_CLUSTER_NAME --overwrite-existing
    log_success "AKS credentials configured"
}

# Setup Azure Container Registry
setup_acr() {
    log_info "Setting up Azure Container Registry..."
    
    # Create ACR if it doesn't exist
    if ! az acr show --name $ACR_NAME &> /dev/null; then
        log_info "Creating Azure Container Registry $ACR_NAME..."
        az acr create \
            --resource-group $RESOURCE_GROUP \
            --name $ACR_NAME \
            --sku Standard \
            --location "$LOCATION"
        log_success "ACR created"
    else
        log_info "ACR $ACR_NAME already exists"
    fi
    
    # Attach ACR to AKS
    log_info "Attaching ACR to AKS cluster..."
    az aks update \
        --resource-group $RESOURCE_GROUP \
        --name $AKS_CLUSTER_NAME \
        --attach-acr $ACR_NAME
    log_success "ACR attached to AKS"
}

# Setup Azure Key Vault and Managed Identity
setup_azure_services() {
    log_info "Setting up Azure services..."
    
    # Create Key Vault
    KV_NAME="ft-keyvault-$(date +%s)"
    log_info "Creating Key Vault $KV_NAME..."
    az keyvault create \
        --name $KV_NAME \
        --resource-group $RESOURCE_GROUP \
        --location "$LOCATION" \
        --sku standard
    
    # Create Managed Identity
    IDENTITY_NAME="file-transfer-identity"
    log_info "Creating Managed Identity $IDENTITY_NAME..."
    az identity create \
        --name $IDENTITY_NAME \
        --resource-group $RESOURCE_GROUP \
        --location "$LOCATION"
    
    # Get identity details
    IDENTITY_CLIENT_ID=$(az identity show --name $IDENTITY_NAME --resource-group $RESOURCE_GROUP --query clientId -o tsv)
    IDENTITY_OBJECT_ID=$(az identity show --name $IDENTITY_NAME --resource-group $RESOURCE_GROUP --query principalId -o tsv)
    
    # Grant Key Vault access to Managed Identity
    log_info "Granting Key Vault access to Managed Identity..."
    az keyvault set-policy \
        --name $KV_NAME \
        --object-id $IDENTITY_OBJECT_ID \
        --secret-permissions get list
    
    # Create federated identity credential for workload identity
    OIDC_ISSUER=$(az aks show --resource-group $RESOURCE_GROUP --name $AKS_CLUSTER_NAME --query "oidcIssuerProfile.issuerUrl" -o tsv)
    
    az identity federated-credential create \
        --name "file-transfer-federated-credential" \
        --identity-name $IDENTITY_NAME \
        --resource-group $RESOURCE_GROUP \
        --issuer $OIDC_ISSUER \
        --subject "system:serviceaccount:$NAMESPACE:file-transfer-sa"
    
    log_success "Azure services setup completed"
    log_info "Key Vault: $KV_NAME"
    log_info "Managed Identity Client ID: $IDENTITY_CLIENT_ID"
}

# Create namespace and RBAC
setup_kubernetes_rbac() {
    log_info "Setting up Kubernetes RBAC..."
    
    # Create namespace
    kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
    
    # Create service account with workload identity annotation
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: file-transfer-sa
  namespace: $NAMESPACE
  annotations:
    azure.workload.identity/client-id: $IDENTITY_CLIENT_ID
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: file-transfer-role
rules:
- apiGroups: [""]
  resources: ["secrets", "configmaps"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: file-transfer-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: file-transfer-role
subjects:
- kind: ServiceAccount
  name: file-transfer-sa
  namespace: $NAMESPACE
EOF
    
    log_success "Kubernetes RBAC configured"
}

# Install required Helm repositories
setup_helm_repos() {
    log_info "Setting up Helm repositories..."
    
    # Add Bitnami repository
    helm repo add bitnami https://charts.bitnami.com/bitnami
    
    # Add ingress-nginx repository
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
    
    # Add cert-manager repository
    helm repo add cert-manager https://charts.jetstack.io
    
    # Update repositories
    helm repo update
    
    log_success "Helm repositories configured"
}

# Install cluster services
install_cluster_services() {
    log_info "Installing cluster services..."
    
    # Install ingress controller
    log_info "Installing NGINX Ingress Controller..."
    helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
        --namespace ingress-nginx \
        --create-namespace \
        --set controller.replicaCount=2 \
        --set controller.nodeSelector."kubernetes\.io/os"=linux \
        --set defaultBackend.nodeSelector."kubernetes\.io/os"=linux \
        --wait
    
    # Install cert-manager
    log_info "Installing cert-manager..."
    helm upgrade --install cert-manager cert-manager/cert-manager \
        --namespace cert-manager \
        --create-namespace \
        --set installCRDs=true \
        --wait
    
    # Create cluster issuer for Let's Encrypt
    cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@yourcompany.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
    
    log_success "Cluster services installed"
}

# Deploy application using Helm
deploy_application() {
    log_info "Deploying File Transfer application..."
    
    # Create values override file
    cat > values-override.yaml <<EOF
global:
  imageRegistry: $ACR_NAME.azurecr.io

configService:
  secrets:
    AZURE_SQL_MI_SERVER: "your-sql-mi-server.database.windows.net"
    AZURE_SQL_MI_DATABASE: "filetransfer"
    AZURE_SQL_MI_USERNAME: "filetransfer"
    AZURE_SQL_MI_PASSWORD: "YourSecurePassword123!"
    AZURE_KEYVAULT_URI: "https://$KV_NAME.vault.azure.net/"
    AZURE_TENANT_ID: "$(az account show --query tenantId -o tsv)"
    AZURE_CLIENT_ID: "$IDENTITY_CLIENT_ID"
    JWT_SECRET: "$(openssl rand -base64 32)"
  
  # Enhanced Configuration
  config:
    ENHANCED_CUTOFF_ENABLED: "true"
    DEFAULT_CUTOFF_TIME_TYPE: "WEEKDAY_WEEKEND"
    SUNDAY_HOLIDAY_ENABLED: "true"
    HOLIDAY_SERVICE_ENABLED: "true"
    AUTO_CREATE_SUNDAY_HOLIDAYS: "true"
    MULTI_TENANT_ENABLED: "true"
    DEFAULT_TENANT_ID: "default"
    TIMEZONE_SUPPORT_ENABLED: "true"
    DEFAULT_TIMEZONE: "UTC"
    PROMETHEUS_ENABLED: "true"
    METRICS_ENDPOINT: "/actuator/prometheus"

azure:
  workloadIdentity:
    enabled: true
    clientId: "$IDENTITY_CLIENT_ID"
  keyVault:
    name: "$KV_NAME"
    resourceGroup: "$RESOURCE_GROUP"
    subscriptionId: "$(az account show --query id -o tsv)"

serviceAccount:
  name: "file-transfer-sa"
  annotations:
    azure.workload.identity/client-id: "$IDENTITY_CLIENT_ID"

gateway:
  ingress:
    hosts:
      - host: filetransfer-dev.yourcompany.com
        paths:
          - path: /
            pathType: Prefix
    tls:
      - secretName: filetransfer-tls
        hosts:
          - filetransfer-dev.yourcompany.com
EOF
    
    # Deploy using Helm
    helm upgrade --install $HELM_RELEASE_NAME $CHART_PATH \
        --namespace $NAMESPACE \
        --values values-override.yaml \
        --wait \
        --timeout 10m
    
    log_success "Application deployed successfully"
}

# Wait for deployment to be ready
wait_for_deployment() {
    log_info "Waiting for deployment to be ready..."
    
    # Wait for all deployments to be ready
    kubectl wait --for=condition=available --timeout=300s deployment --all -n $NAMESPACE
    
    # Get service status
    kubectl get all -n $NAMESPACE
    
    # Get ingress IP
    INGRESS_IP=$(kubectl get svc ingress-nginx-controller -n ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
    
    log_success "Deployment is ready!"
    log_info "Ingress IP: $INGRESS_IP"
    log_info "Access the application at: http://$INGRESS_IP (or configure DNS)"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up temporary files..."
    rm -f values-override.yaml
}

# Main deployment flow
main() {
    log_info "Starting File Transfer Microservices deployment to AKS..."
    
    check_prerequisites
    setup_aks_cluster
    setup_acr
    setup_azure_services
    setup_kubernetes_rbac
    setup_helm_repos
    install_cluster_services
    deploy_application
    wait_for_deployment
    cleanup
    
    log_success "Deployment completed successfully!"
    log_info "Next steps:"
    log_info "1. Update DNS to point to the ingress IP"
    log_info "2. Configure Azure SQL MI connection strings"
    log_info "3. Set up monitoring and alerting"
    log_info "4. Configure backup policies"
}

# Handle script interruption
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi