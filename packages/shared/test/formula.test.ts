import{test}from"node:test";import{strict as assert}from"node:assert";import{evaluateFormula}from"../src/formula.js";
const cells:Record<string,string>={A1:"10",A2:"20",A3:"30",B1:"2"};const other:Record<string,string>={A1:"5",A2:"7"};
const get=(sheet:string|undefined,key:string)=>sheet==="Outra"?other[key]:cells[key];
test("arithmetic and A1 references",()=>assert.equal(evaluateFormula("=A1+B1*2",get,"Planilha 1").value,14));
test("range functions",()=>assert.equal(evaluateFormula("=SUM(A1:A3)",get,"Planilha 1").value,60));
test("average min max count",()=>{assert.equal(evaluateFormula("=AVERAGE(A1:A3)",get,"Planilha 1").value,20);assert.equal(evaluateFormula("=MIN(A1:A3)",get,"Planilha 1").value,10);assert.equal(evaluateFormula("=MAX(A1:A3)",get,"Planilha 1").value,30);assert.equal(evaluateFormula("=COUNT(A1:A3)",get,"Planilha 1").value,3)});
test("cross sheet references",()=>assert.equal(evaluateFormula("='Outra'!A1+Outra!A2",get,"Planilha 1").value,12));
test("invalid formulas return error",()=>assert.equal(evaluateFormula("=A1+",get,"Planilha 1").value,"#ERROR!"));

test("recursive formula dependencies",()=>{const map:Record<string,string>={A1:"10",A2:"=A1*2",A3:"=A2+5"};const get=(s:string|undefined,k:string)=>map[k];assert.equal(evaluateFormula("=A3",get,"Planilha 1").value,25)});
test("circular references return CIRC",()=>{const map:Record<string,string>={A1:"=A2",A2:"=A1"};const get=(s:string|undefined,k:string)=>map[k];assert.equal(evaluateFormula("=A1",get,"Planilha 1").value,"#CIRC!")});
test("COUNT counts only numeric values",()=>{const map:Record<string,string>={A1:"1",A2:"texto",A3:"",A4:"0"};const get=(s:string|undefined,k:string)=>map[k];assert.equal(evaluateFormula("=COUNT(A1:A4)",get,"Planilha 1").value,2)});
test("aggregate functions resolve formula cells",()=>{const map:Record<string,string>={A1:"10",A2:"=A1*2",A3:"=A2+5"};const get=(s:string|undefined,k:string)=>map[k];assert.equal(evaluateFormula("=SUM(A1:A3)",get,"Planilha 1").value,55);assert.equal(evaluateFormula("=AVERAGE(A2:A3)",get,"Planilha 1").value,17.5)});
