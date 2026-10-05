import{readFile}from"node:fs/promises";
const p="apps/android/app/src/main/java/com/korczaktech/nexa/MainActivity.kt";const s=await readFile(p,"utf8");
for(const x of ["FormulaParser","SUM","AVERAGE","MIN","MAX","COUNT","CIRCULAR","shiftFormula","joinToString(\"\\t\")","ACTION_OPEN_DOCUMENT","fromDelimited","frozenRows","frozenCols","borderTop","conditionalRules","validationRules","groupedRows","groupedCols"])if(!s.includes(x))throw Error("Android Phase 1 missing: "+x);
if(/setPrimaryClip\(ClipData\.newPlainText\("Nexa",book!!\.sheets/.test(s))throw Error("Android clipboard is single-cell");
console.log("Android Phase 1 source verification: OK");