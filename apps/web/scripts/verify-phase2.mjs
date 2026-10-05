import{readFileSync}from"node:fs";
const app=readFileSync(new URL("../src/App.tsx",import.meta.url),"utf8"),panel=readFileSync(new URL("../src/Phase2Panel.tsx",import.meta.url),"utf8");
for(const x of ["Phase2Panel","Nexa Completo","phase2Open"])if(!app.includes(x))throw new Error("Web não integrado: "+x);
for(const x of ["Gráficos","Tabela dinâmica","Compartilhamento","Comentários","Versões","Templates"])if(!panel.includes(x))throw new Error("Fase 2 web ausente: "+x);
console.log("phase2 web verification: PASS");