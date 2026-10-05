import{readFileSync}from"node:fs";
const route=readFileSync(new URL("./routes/phase2.js",import.meta.url),"utf8");
const required=["/charts","/tables","/pivots","/dashboards","/comments","/shares","/versions","/sync","/analysis","/versions/:versionId/restore","/v1/templates"];
for(const x of required)if(!route.includes(x))throw new Error("Fase 2 API ausente: "+x);
if(route.includes('dropDatabase')||route.includes('Contas'))throw new Error("Fase 2 tentou operar destrutivamente na database global.");
console.log("phase2 api verification: PASS");