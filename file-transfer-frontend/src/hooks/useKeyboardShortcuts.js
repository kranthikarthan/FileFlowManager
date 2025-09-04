import { useEffect, useCallback } from 'react';

/**
 * Custom hook for keyboard shortcuts
 */
export const useKeyboardShortcuts = (shortcuts) => {
  const handleKeyPress = useCallback((event) => {
    const { key, ctrlKey, altKey, shiftKey, metaKey } = event;
    
    // Create a key combination string
    const modifiers = [];
    if (ctrlKey || metaKey) modifiers.push('ctrl');
    if (altKey) modifiers.push('alt');
    if (shiftKey) modifiers.push('shift');
    
    const combination = [...modifiers, key.toLowerCase()].join('+');
    
    // Check if we have a handler for this combination
    const handler = shortcuts[combination];
    if (handler && typeof handler === 'function') {
      event.preventDefault();
      handler(event);
    }
  }, [shortcuts]);

  useEffect(() => {
    document.addEventListener('keydown', handleKeyPress);
    
    return () => {
      document.removeEventListener('keydown', handleKeyPress);
    };
  }, [handleKeyPress]);
};

/**
 * Predefined keyboard shortcuts for file transfer operations
 */
export const fileTransferShortcuts = {
  // Navigation shortcuts
  'ctrl+1': () => window.location.hash = '#/',
  'ctrl+2': () => window.location.hash = '#/transfers',
  'ctrl+3': () => window.location.hash = '#/service-config',
  'ctrl+4': () => window.location.hash = '#/tenants',
  
  // File operation shortcuts
  'ctrl+r': () => window.location.reload(),
  'f5': () => window.location.reload(),
  'ctrl+f': () => {
    const searchInput = document.querySelector('input[placeholder*="Search"], input[placeholder*="Filter"]');
    if (searchInput) {
      searchInput.focus();
    }
  },
  
  // Bulk operation shortcuts
  'ctrl+a': (event) => {
    const selectAllButton = document.querySelector('[data-testid="select-all-button"]');
    if (selectAllButton) {
      event.preventDefault();
      selectAllButton.click();
    }
  },
  
  'delete': () => {
    const deleteButton = document.querySelector('[data-testid="bulk-delete-button"]');
    if (deleteButton && !deleteButton.disabled) {
      deleteButton.click();
    }
  },
  
  'ctrl+shift+t': () => {
    const tagButton = document.querySelector('[data-testid="bulk-tag-button"]');
    if (tagButton) {
      tagButton.click();
    }
  },
  
  // Dialog shortcuts
  'escape': () => {
    const closeButton = document.querySelector('[data-testid="dialog-close"], .MuiDialog-root .MuiIconButton-root');
    if (closeButton) {
      closeButton.click();
    }
  },
  
  'enter': (event) => {
    if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
      const submitButton = document.querySelector('[data-testid="dialog-submit"], .MuiDialog-root .MuiButton-contained');
      if (submitButton && !submitButton.disabled) {
        submitButton.click();
      }
    }
  }
};

/**
 * Hook for file transfer specific shortcuts
 */
export const useFileTransferShortcuts = (customShortcuts = {}) => {
  const allShortcuts = { ...fileTransferShortcuts, ...customShortcuts };
  useKeyboardShortcuts(allShortcuts);
};

export default useKeyboardShortcuts;