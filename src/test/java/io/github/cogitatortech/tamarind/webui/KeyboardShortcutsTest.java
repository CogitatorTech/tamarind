package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for keyboard shortcuts functionality */
public class KeyboardShortcutsTest {

  @Test
  public void testShortcutsJsExists() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(
        shortcutsJs.contains("const KeyboardShortcuts"),
        "shortcuts.js should define KeyboardShortcuts");
    assertTrue(
        shortcutsJs.contains("window.KeyboardShortcuts = KeyboardShortcuts"),
        "KeyboardShortcuts should be globally available");
  }

  @Test
  public void testShortcutsDataStructure() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(
        shortcutsJs.contains("shortcuts:"), "KeyboardShortcuts should have shortcuts property");

    // Check for main categories
    assertTrue(shortcutsJs.contains("'Global'"), "Should have Global shortcuts category");
    assertTrue(shortcutsJs.contains("'Notebook'"), "Should have Notebook shortcuts category");
    assertTrue(shortcutsJs.contains("'Navigation'"), "Should have Navigation shortcuts category");
    assertTrue(shortcutsJs.contains("'Results'"), "Should have Results shortcuts category");
  }

  @Test
  public void testKeyShortcuts() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");

    // Check for common shortcuts
    assertTrue(shortcutsJs.contains("Shift+Enter"), "Should document Shift+Enter shortcut");
    assertTrue(shortcutsJs.contains("Ctrl/Cmd+Enter"), "Should document Ctrl+Enter shortcut");
    assertTrue(shortcutsJs.contains("Ctrl+Space"), "Should document Ctrl+Space shortcut");
    assertTrue(shortcutsJs.contains("Ctrl/Cmd+S"), "Should document Ctrl+S shortcut");
  }

  @Test
  public void testQuestionMarkTrigger() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("e.key === '?'"), "Should listen for ? key to show shortcuts");
    assertTrue(shortcutsJs.contains("key: '?'"), "Should document ? key in shortcuts list");
  }

  @Test
  public void testModalCreation() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("createModal"), "Should have createModal method");
    assertTrue(
        shortcutsJs.contains("shortcuts-modal"), "Should create modal with shortcuts-modal class");
  }

  @Test
  public void testSearchFunctionality() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("shortcuts-search-input"), "Should have search input");
    assertTrue(shortcutsJs.contains("filterShortcuts"), "Should have filterShortcuts method");
    assertTrue(shortcutsJs.contains("input"), "Should listen to input events for search");
  }

  @Test
  public void testShowAndCloseMethods() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("show()"), "Should have show method");
    assertTrue(shortcutsJs.contains("close()"), "Should have close method");
    assertTrue(shortcutsJs.contains("isOpen()"), "Should have isOpen method");
  }

  @Test
  public void testEscapeKeyCloses() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("e.key === 'Escape'"), "Should listen for Escape key");
    assertTrue(shortcutsJs.contains("this.close()"), "Escape should close modal");
  }

  @Test
  public void testInputFocusCheck() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("isInputFocused"), "Should check if input is focused");
    assertTrue(
        shortcutsJs.contains("INPUT") && shortcutsJs.contains("TEXTAREA"),
        "Should check for INPUT and TEXTAREA elements");
    assertTrue(shortcutsJs.contains("CodeMirror"), "Should check for CodeMirror editor focus");
  }

  @Test
  public void testKeyboardStyling() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("<kbd>"), "Should use <kbd> elements for keyboard keys");
  }

  @Test
  public void testCategorizedShortcuts() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("shortcuts-category"), "Should group shortcuts by category");
    assertTrue(shortcutsJs.contains("shortcuts-category-title"), "Should have category titles");
  }

  @Test
  public void testResponsiveDesign() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("@media"), "Should have responsive styles");
    assertTrue(shortcutsJs.contains("768px"), "Should have mobile breakpoint");
  }

  @Test
  public void testAccessibility() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("aria-label"), "Should have ARIA labels");
    assertTrue(shortcutsJs.contains("focus()"), "Should manage focus");
  }

  @Test
  public void testPrintStyles() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("@media print"), "Should have print-friendly styles");
  }

  @Test
  public void testComingSoonFeatures() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(
        shortcutsJs.contains("comingSoon"), "Should support marking features as coming soon");
    assertTrue(shortcutsJs.contains("coming-soon-badge"), "Should have coming soon badge styling");
  }

  @Test
  public void testDashboardReferencesShortcuts() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");

    assertNotNull(dashboardHtml, "dashboard.html should exist");
    assertTrue(
        dashboardHtml.contains("shortcuts.js"), "dashboard.html should reference shortcuts.js");
  }

  @Test
  public void testNotebookReferencesShortcuts() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("shortcuts.js"), "notebook.html should reference shortcuts.js");
  }

  @Test
  public void testNotebookHasShortcutsButton() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("KeyboardShortcuts.show()"),
        "notebook should have button to show shortcuts");
    assertTrue(notebookHtml.contains("⌨️"), "shortcuts button should have keyboard icon");
  }

  @Test
  public void testEmptyStateHandling() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(shortcutsJs.contains("shortcuts-empty"), "Should handle empty search results");
    assertTrue(shortcutsJs.contains("No shortcuts found"), "Should show empty state message");
  }

  @Test
  public void testAnimations() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(
        shortcutsJs.contains("@keyframes shortcuts-slide-in"), "Should have slide-in animation");
    assertTrue(shortcutsJs.contains("animation:"), "Should apply animations");
  }

  @Test
  public void testAddShortcutMethod() {
    String shortcutsJs = readResourceFile("/META-INF/resources/assets/js/shortcuts.js");

    assertNotNull(shortcutsJs, "shortcuts.js should exist");
    assertTrue(
        shortcutsJs.contains("addShortcut(category, key, description"),
        "Should have addShortcut method for extensibility");
  }

  private String readResourceFile(String path) {
    try {
      var resource = getClass().getResourceAsStream(path);
      if (resource == null) {
        return null;
      }
      return new String(resource.readAllBytes());
    } catch (Exception e) {
      return null;
    }
  }
}
