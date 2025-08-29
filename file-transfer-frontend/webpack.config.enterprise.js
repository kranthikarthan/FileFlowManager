/**
 * Enterprise Performance Webpack Configuration
 * Optimized for large datasets, high concurrency, and enterprise-scale operations
 */

const path = require('path');
const webpack = require('webpack');
const TerserPlugin = require('terser-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const WorkboxPlugin = require('workbox-webpack-plugin');

module.exports = {
  mode: 'production',
  
  // Entry points optimized for code splitting
  entry: {
    main: './src/index.js',
    vendor: ['react', 'react-dom', '@mui/material'],
    charts: ['recharts'],
    utils: ['lodash', 'date-fns']
  },
  
  // Output configuration for enterprise
  output: {
    path: path.resolve(__dirname, 'build'),
    filename: 'static/js/[name].[contenthash:8].js',
    chunkFilename: 'static/js/[name].[contenthash:8].chunk.js',
    publicPath: '/',
    clean: true
  },
  
  // Optimization for enterprise performance
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          // Aggressive optimization for enterprise
          compress: {
            drop_console: true,          // Remove console.log in production
            drop_debugger: true,         // Remove debugger statements
            pure_funcs: ['console.log', 'console.info', 'console.debug'],
            passes: 3                    // Multiple optimization passes
          },
          mangle: {
            safari10: true               // Safari 10 compatibility
          },
          format: {
            comments: false              // Remove comments
          }
        },
        extractComments: false,          // Don't extract license comments
        parallel: true                   // Parallel minification
      })
    ],
    
    // Code splitting for enterprise
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        // Vendor libraries
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all',
          priority: 20,
          reuseExistingChunk: true
        },
        
        // Common components
        common: {
          name: 'common',
          minChunks: 2,
          chunks: 'all',
          priority: 10,
          reuseExistingChunk: true,
          enforce: true
        },
        
        // Large UI libraries
        mui: {
          test: /[\\/]node_modules[\\/]@mui[\\/]/,
          name: 'mui',
          chunks: 'all',
          priority: 30
        },
        
        // Chart libraries (loaded on demand)
        charts: {
          test: /[\\/]node_modules[\\/](recharts|d3)[\\/]/,
          name: 'charts',
          chunks: 'async',
          priority: 15
        },
        
        // Utility libraries
        utils: {
          test: /[\\/]node_modules[\\/](lodash|date-fns|uuid)[\\/]/,
          name: 'utils',
          chunks: 'all',
          priority: 25
        }
      }
    },
    
    // Runtime chunk for better caching
    runtimeChunk: {
      name: 'runtime'
    }
  },
  
  // Module configuration for enterprise
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: [
              ['@babel/preset-env', {
                targets: {
                  browsers: ['> 1%', 'last 2 versions']
                },
                modules: false,
                useBuiltIns: 'usage',
                corejs: 3
              }],
              ['@babel/preset-react', {
                runtime: 'automatic'
              }]
            ],
            plugins: [
              // Performance optimizations
              '@babel/plugin-transform-runtime',
              ['import', {
                libraryName: '@mui/material',
                libraryDirectory: '',
                camel2DashComponentName: false
              }, 'core'],
              ['import', {
                libraryName: '@mui/icons-material',
                libraryDirectory: '',
                camel2DashComponentName: false
              }, 'icons']
            ],
            cacheDirectory: true,           // Enable babel cache
            cacheCompression: false         // Disable compression for speed
          }
        }
      },
      
      {
        test: /\.css$/,
        use: [
          'style-loader',
          {
            loader: 'css-loader',
            options: {
              modules: {
                localIdentName: '[hash:base64:8]' // Short class names for performance
              }
            }
          }
        ]
      },
      
      {
        test: /\.(png|jpe?g|gif|svg)$/,
        type: 'asset',
        parser: {
          dataUrlCondition: {
            maxSize: 8192 // 8KB inline limit
          }
        },
        generator: {
          filename: 'static/media/[name].[contenthash:8][ext]'
        }
      }
    ]
  },
  
  // Plugins for enterprise performance
  plugins: [
    // Environment variables for enterprise
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('production'),
      'process.env.REACT_APP_ENTERPRISE_MODE': JSON.stringify('true'),
      'process.env.REACT_APP_PERFORMANCE_OPTIMIZED': JSON.stringify('true')
    }),
    
    // Compression for enterprise bandwidth optimization
    new CompressionPlugin({
      algorithm: 'gzip',
      test: /\.(js|css|html|svg)$/,
      threshold: 8192,               // Compress files > 8KB
      minRatio: 0.8,                 // Only compress if 20%+ reduction
      compressionOptions: {
        level: 9                     // Maximum compression
      }
    }),
    
    // Brotli compression for modern browsers
    new CompressionPlugin({
      filename: '[path][base].br',
      algorithm: 'brotliCompress',
      test: /\.(js|css|html|svg)$/,
      threshold: 8192,
      minRatio: 0.8,
      compressionOptions: {
        level: 11                    // Maximum brotli compression
      }
    }),
    
    // Service worker for enterprise PWA features
    new WorkboxPlugin.GenerateSW({
      clientsClaim: true,
      skipWaiting: true,
      maximumFileSizeToCacheInBytes: 10 * 1024 * 1024, // 10MB cache limit
      
      // Enterprise caching strategies
      runtimeCaching: [
        {
          urlPattern: /^https:\/\/api\./,
          handler: 'NetworkFirst',
          options: {
            cacheName: 'api-cache',
            expiration: {
              maxEntries: 1000,        // Large cache for enterprise
              maxAgeSeconds: 300       // 5 minutes for API data
            },
            cacheKeyWillBeUsed: async ({ request }) => {
              // Custom cache key for tenant isolation
              const url = new URL(request.url);
              const tenantId = url.searchParams.get('tenantId') || 'default';
              return `${tenantId}-${request.url}`;
            }
          }
        },
        
        {
          urlPattern: /\.(?:png|jpg|jpeg|svg|gif)$/,
          handler: 'CacheFirst',
          options: {
            cacheName: 'images-cache',
            expiration: {
              maxEntries: 500,
              maxAgeSeconds: 86400     // 24 hours for images
            }
          }
        },
        
        {
          urlPattern: /^https:\/\/fonts\./,
          handler: 'StaleWhileRevalidate',
          options: {
            cacheName: 'fonts-cache',
            expiration: {
              maxEntries: 100,
              maxAgeSeconds: 604800    // 1 week for fonts
            }
          }
        }
      ]
    }),
    
    // Bundle analyzer for enterprise optimization (development only)
    ...(process.env.ANALYZE_BUNDLE === 'true' ? [
      new BundleAnalyzerPlugin({
        analyzerMode: 'static',
        openAnalyzer: false,
        reportFilename: 'bundle-analysis.html'
      })
    ] : [])
  ],
  
  // Resolve configuration for enterprise
  resolve: {
    extensions: ['.js', '.jsx', '.json'],
    alias: {
      '@': path.resolve(__dirname, 'src'),
      'components': path.resolve(__dirname, 'src/components'),
      'services': path.resolve(__dirname, 'src/services'),
      'utils': path.resolve(__dirname, 'src/utils'),
      'hooks': path.resolve(__dirname, 'src/hooks')
    },
    
    // Symlinks resolution
    symlinks: false,
    
    // Module resolution optimization
    modules: ['node_modules']
  },
  
  // Performance optimization
  performance: {
    hints: 'warning',
    maxEntrypointSize: 2097152,    // 2MB entrypoint limit
    maxAssetSize: 1048576,         // 1MB asset limit
    assetFilter: (assetFilename) => {
      // Only warn for JS and CSS files
      return assetFilename.endsWith('.js') || assetFilename.endsWith('.css');
    }
  },
  
  // DevTool configuration
  devtool: false, // Disable source maps in production for performance
  
  // Stats configuration for enterprise builds
  stats: {
    colors: true,
    modules: false,
    chunks: false,
    chunkModules: false,
    children: false,
    warnings: true,
    errors: true,
    errorDetails: true,
    performance: true,
    timings: true,
    builtAt: true
  }
};