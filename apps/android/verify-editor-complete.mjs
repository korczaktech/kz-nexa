import{readFileSync}from"node:fs";
const p=readFileSync(new URL("./app/src/main/java/com/korczaktech/nexa/MainActivity.kt",import.meta.url),"utf8");
const checks=[
 ["1 Undo/Redo",["EditorHistory","private fun snap()","private fun undo()","private fun redo()","history.size>100"]],
 ["2 Clipboard",["private fun copy()","private fun cut()","private fun paste()"]],
 ["3 Formula bar",["syncFormulaEditor()","updateCellFromFormula(","commitFormulaEditor()","formulaEditor"]],
 ["4 Selection",["selStart","selEnd","selColStart","selColEnd","ACTION_MOVE"]],
 ["5 Dimensions",["columnWidths","rowHeights","colWidth(","rowHeight(","resizeColumn()","resizeRow()"]],
 ["6 Sort/filter",["sortSelection()","filterSelection()"]],
 ["7 Formatting",["toggle(","fontSize()","wrap()","numberFormat()","border()","conditional()","validation()"]],
 ["8 Merge",["merge()","unmerge()","rangeOverlaps"]],
 ["9 AutoFill",["autoFill()","shiftFormula(","formatSeriesNumber("]],
 ["9b AutoFill",["autoFill()","formatSeriesNumber("]],
 ["10 Rows/columns",["insertRows()","insertCols()","deleteRows()","deleteCols()"]],
 ["11 Freeze/hide/group",["freeze()","hide()","show()","groupRow()","groupCol()","toggleGroups()"]],
 ["12 Keyboard",["setOnKeyListener","KEYCODE_C","KEYCODE_X","KEYCODE_V","KEYCODE_Z","KEYCODE_Y","KEYCODE_DPAD_LEFT","KEYCODE_DPAD_RIGHT","KEYCODE_DPAD_UP","KEYCODE_DPAD_DOWN"]],
 ["13 Exit editor",["pageBack()","Fechar editor"]],
 ["14 Legible cells",["val ink=if(dark)","ce.fontColor==Color.DKGRAY","Color.rgb(35,48,41)"]]
];
for(const [name,need] of checks)for(const token of need)if(!p.includes(token))throw new Error(name+" ausente: "+token);
if(/WebView|loadUrl\(/.test(p))throw new Error("Editor Android não pode depender de WebView.");
console.log("editor completeness verification: PASS — editor functional blocks present");
