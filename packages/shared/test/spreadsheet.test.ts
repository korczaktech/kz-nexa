import{test}from"node:test";import{strict as assert}from"node:assert";import{cellKey,columnIndex,columnName,expandRange,formatNumber,parseCsv,shiftFormulaReferences}from"../src/spreadsheet.js";
test("cell coordinates",()=>{assert.equal(cellKey(0,0),"A1");assert.equal(cellKey(27,27),"AB28");assert.equal(columnIndex("AB"),27);assert.equal(columnName(27),"AB");assert.deepEqual(expandRange("B2","C3"),["B2","C2","B3","C3"])});
test("CSV quotes commas and newlines",()=>assert.deepEqual(parseCsv('a,"b,c","d""e"\n1,2,3'),[["a","b,c",'d"e'],["1","2","3"]]));
test("number formats",()=>{assert.match(formatNumber(1234.5,"currency"),/R\$/);assert.match(formatNumber(.25,"percent"),/%/);assert.equal(formatNumber(12.5,"number"),"12,5")});

test("relative formula shifting",()=>{assert.equal(shiftFormulaReferences("=A1+B$2+$C3+$D$4",1,2),"=C2+D$2+$C4+$D$4")});
