package dh.tool.swing;

import org.fife.rsta.ui.CollapsibleSectionPanel;
import org.fife.rsta.ui.search.ReplaceToolBar;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Add find and replace function to {@link org.fife.ui.rsyntaxtextarea.RSyntaxTextArea}
 * Created by hiep on 29/06/2014.
 */
public class CodeEditor extends CollapsibleSectionPanel implements SearchListener {
	private final RSyntaxTextArea internalEditor;
	private final ReplaceToolBar replaceToolBar = new ReplaceToolBar(this);
	//private final ReplaceDialog replaceDialog;

	public CodeEditor() {
		this(0, 0, SyntaxConstants.SYNTAX_STYLE_NONE);
	}
	public CodeEditor(int rows, int columns, String syntax) {
		super(true);

//		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
//		replaceDialog = new ReplaceDialog(topFrame, this);
//		replaceDialog.setSearchContext(replaceToolBar.getSearchContext()); // This ties the properties of the two dialogs together (match case, regex, etc.).

		internalEditor = new RSyntaxTextArea(rows, columns);
		internalEditor.setSyntaxEditingStyle(syntax);
		internalEditor.setCodeFoldingEnabled(true);
		internalEditor.setMarkOccurrences(true);

		this.add(new RTextScrollPane(internalEditor));
		this.addBottomComponent(ctrl_F(), replaceToolBar);
	}

	private KeyStroke ctrl_shift_H() {
		int ctrl = getToolkit().getMenuShortcutKeyMask();
		int shift = InputEvent.SHIFT_MASK;
		return KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrl | shift);
	}
	private KeyStroke ctrl_F() {
		int ctrl = getToolkit().getMenuShortcutKeyMask();
		return KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl);
	}
	private KeyStroke ctrl_H() {
		int ctrl = getToolkit().getMenuShortcutKeyMask();
		return KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrl);
	}

	@Override
	public void searchEvent(SearchEvent searchEvent) {
		SearchEvent.Type type = searchEvent.getType();
		SearchContext context = searchEvent.getSearchContext();
		SearchResult result = null;

		switch (type) {
			case MARK_ALL:
				result = SearchEngine.markAll(internalEditor, context);
				break;
			case FIND:
				result = SearchEngine.find(internalEditor, context);
				if (!result.wasFound()) {
					UIManager.getLookAndFeel().provideErrorFeedback(internalEditor);
				}
				break;
			case REPLACE:
				result = SearchEngine.replace(internalEditor, context);
				if (!result.wasFound()) {
					UIManager.getLookAndFeel().provideErrorFeedback(internalEditor);
				}
				break;
			case REPLACE_ALL:
				result = SearchEngine.replaceAll(internalEditor, context);
				JOptionPane.showMessageDialog(null, result.getCount() +
						" occurrences replaced.");
				break;
		}

//		String text = null;
//		if (result.wasFound()) {
//			text = "Text found; occurrences marked: " + result.getMarkedCount();
//		}
//		else if (type==SearchEvent.Type.MARK_ALL) {
//			if (result.getMarkedCount()>0) {
//				text = "Occurrences marked: " + result.getMarkedCount();
//			}
//			else {
//				text = "";
//			}
//		}
//		else {
//			text = "Text not found";
//		}
//		statusBar.setLabel(text);
	}

	@Override
	public String getSelectedText() {
		return internalEditor.getSelectedText();
	}

	public RSyntaxTextArea getInternalEditor() {
		return internalEditor;
	}

	public String getText() {
		return internalEditor.getText();
	}

	public void setText(String text) {
		internalEditor.setText(text);
	}

	public void setSyntaxEditingStyle(String style) {
		internalEditor.setSyntaxEditingStyle(style);
	}
}
