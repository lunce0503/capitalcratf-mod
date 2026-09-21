// Shared source for Seia's runtime mesh, editable OBJ/MTL, palette texture and metadata.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { writePng } from './mari-png.mjs';

const base = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const output = path.join(base, 'src/main/resources/assets/capitalcraft/models/entity/resident/seia');
const palette = {
  white: '#f7f4ee', whiteLight: '#fffdf8', whiteShade: '#d8dbe4', blueWhite: '#dbe7f4',
  skin: '#ffe4d0', skinShade: '#edc4b4', blush: '#f2b8b5', mouth: '#c98787',
  hair: '#f2d397', hairLight: '#ffebbd', hairShade: '#dcb979', hairDeep: '#c89b62',
  ear: '#eab18f', earInner: '#fff4e7', tailTip: '#fff0cf',
  navy: '#263b5a', navyLight: '#38577b', blue: '#779bc8', blueLight: '#b5cee8',
  gold: '#d6b76f', goldLight: '#f4dda0', goldShade: '#aa8950',
  eye: '#d294b1', eyeLight: '#f2cad9', eyeShade: '#9d657f', ink: '#796273',
  halo: '#eed58d', haloLight: '#fff1b8', shoe: '#e8e4e2', sole: '#a99c91',
  bird: '#eef7f8', birdBlue: '#8fb7c9', beak: '#d7a25a'
};
const bones = {
  body: [0, 0, 0], head: [0, 1.63, 0], hair: [0, 1.63, 0],
  left_arm: [0.34, 1.48, 0], right_arm: [-0.34, 1.48, 0],
  left_leg: [0.14, 0.51, 0], right_leg: [-0.14, 0.51, 0],
  tail: [0, 1.03, -0.18], halo: [0, 2.68, 0]
};
const objects = [];
const sub = (a,b) => a.map((v,i)=>v-b[i]);
const cross = (a,b) => [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const dot = (a,b) => a.reduce((s,v,i)=>s+v*b[i],0);
const norm = a => { const n=Math.hypot(...a); return a.map(v=>v/n); };
const mean = points => [0,1,2].map(i=>points.reduce((s,p)=>s+p[i],0)/points.length);
function mesh(name, bone, vertices, faces, material, shades = []) {
  const center=mean(vertices);
  const polygons=faces.map((indices,i)=>{
    indices=[...indices];
    const n=cross(sub(vertices[indices[1]],vertices[indices[0]]),sub(vertices[indices[2]],vertices[indices[0]]));
    if(dot(n,sub(mean(indices.map(j=>vertices[j])),center))<0) indices.reverse();
    return {indices, material:shades[i%shades.length]??material};
  });
  objects.push({name,bone,vertices:vertices.map(p=>p.map(v=>+v.toFixed(5))),faces:polygons});
}
function rings(name,bone,sections,material,sides=10,shades=[],offset=0) {
  const vertices=sections.flatMap(([x,y,z,rx,rz])=>Array.from({length:sides},(_,i)=>{
    const a=2*Math.PI*i/sides+offset; return [x+Math.sin(a)*rx,y,z+Math.cos(a)*rz];
  }));
  const faces=[];
  for(let j=0;j<sections.length-1;j++) for(let i=0;i<sides;i++) faces.push([j*sides+i,j*sides+(i+1)%sides,(j+1)*sides+(i+1)%sides,(j+1)*sides+i]);
  for(const j of [0,sections.length-1]) for(let i=1;i<sides-1;i++) faces.push([j*sides,j*sides+i,j*sides+i+1]);
  mesh(name,bone,vertices,faces,material,shades);
}
function tube(name,bone,sections,materials,sides=10) {
  const vertices=[];
  for(let j=0;j<sections.length;j++) {
    const [x,y,z,r]=sections[j];
    const previous=sections[Math.max(0,j-1)], next=sections[Math.min(sections.length-1,j+1)];
    const tangent=norm(sub(next,previous));
    let across=norm(cross([0,0,1],tangent));
    if(!across.every(Number.isFinite)) across=[1,0,0];
    const depth=norm(cross(tangent,across));
    for(let i=0;i<sides;i++) { const a=2*Math.PI*i/sides; vertices.push([x+across[0]*Math.cos(a)*r+depth[0]*Math.sin(a)*r,y+across[1]*Math.cos(a)*r+depth[1]*Math.sin(a)*r,z+across[2]*Math.cos(a)*r+depth[2]*Math.sin(a)*r]); }
  }
  const faces=[];
  for(let j=0;j<sections.length-1;j++) for(let i=0;i<sides;i++) faces.push({indices:[j*sides+i,j*sides+(i+1)%sides,(j+1)*sides+(i+1)%sides,(j+1)*sides+i],material:materials[Math.min(j,materials.length-1)]});
  for(const j of [0,sections.length-1]) for(let i=1;i<sides-1;i++) faces.push({indices:[j*sides,j*sides+i,j*sides+i+1],material:materials[j===0?0:materials.length-1]});
  objects.push({name,bone,vertices:vertices.map(p=>p.map(v=>+v.toFixed(5))),faces});
}
function box(name,bone,[x,y,z],[w,h,d],mat,rz=0) {
  const corners=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]];
  const vertices=corners.map(([a,b,c])=>[x+a*w/2*Math.cos(rz)-b*h/2*Math.sin(rz),y+a*w/2*Math.sin(rz)+b*h/2*Math.cos(rz),z+c*d/2]);
  mesh(name,bone,vertices,[[0,3,2,1],[4,5,6,7],[0,4,7,3],[1,2,6,5],[3,7,6,2],[0,1,5,4]],mat);
}
function panel(name,bone,points,mat,depth=.014) {
  const vertices=[...points,...points.map(([x,y,z])=>[x,y,z-depth])],n=points.length,faces=[];
  for(let i=1;i<n-1;i++){faces.push([0,i,i+1]);faces.push([n,n+i+1,n+i]);}
  for(let i=0;i<n;i++)faces.push([i,(i+1)%n,(i+1)%n+n,i+n]);
  mesh(name,bone,vertices,faces,mat);
}
function jewel(name,bone,[x,y,z],r,mat='goldLight') {
  mesh(name,bone,[[x-r,y,z],[x,y+r,z],[x+r,y,z],[x,y-r,z],[x,y,z+.045]],[[0,1,4],[1,2,4],[2,3,4],[3,0,4],[0,3,2,1]],mat);
}
function lock(name,bone,sections,material='hair') { rings(name,bone,sections,material,8,[material,'hairLight',material,'hairShade','hairShade',material,'hairLight',material]); }

// Shoes, stockings and separate leg bones for walking.
for(const s of [-1,1]) {
  const bone=s<0?'right_leg':'left_leg',x=s*.14;
  rings(`${bone}_sole`,bone,[[x,.025,.07,.105,.176],[x,.057,.07,.11,.181]],'sole',8);
  rings(`${bone}_shoe`,bone,[[x,.05,.07,.104,.174],[x,.135,.045,.099,.156],[x,.172,.005,.074,.09]],'shoe',8,['shoe','whiteLight','shoe','whiteShade']);
  box(`${bone}_strap`,bone,[x,.146,.111],[.176,.035,.052],'gold');
  rings(`${bone}_stocking`,bone,[[x,.15,0,.078,.084],[x,.65,0,.083,.087]],'white',8,['white','whiteLight','white','whiteShade']);
  box(`${bone}_stocking_ribbon`,bone,[x,.592,.09],[.16,.075,.025],'blueLight');
}

// Layered white skirt, blue-grey piping and warm gold embroidery.
rings('lower_skirt','body',[[0,.47,0,.48,.39],[0,.60,0,.54,.42],[0,1.22,0,.32,.255],[0,1.42,0,.265,.22]],'white',14,['white','whiteLight','white','whiteShade']);
rings('ruffled_hem','body',[[0,.48,.012,.56,.435],[0,.57,.012,.49,.39]],'whiteLight',16,['whiteLight','white','whiteShade']);
rings('blue_hem_band','body',[[0,.606,.023,.497,.394],[0,.647,.023,.472,.374]],'blueLight',16,['blueLight','blueWhite','blueLight','blue']);
rings('gold_hem_line','body',[[0,.665,.026,.458,.36],[0,.686,.026,.445,.35]],'goldLight',16,['gold','goldLight','gold','goldShade']);
rings('waist','body',[[0,1.20,0,.325,.258],[0,1.44,0,.277,.224]],'white',12,['white','whiteLight','white','whiteShade']);
box('waist_gold_belt','body',[0,1.235,.205],[.47,.052,.055],'gold');
panel('navy_bodice','body',[[-.16,1.22,.236],[.16,1.22,.236],[.13,1.62,.205],[0,1.72,.211],[-.13,1.62,.205]],'navy');
panel('white_collar_left','body',[[-.22,1.63,.22],[-.03,1.48,.252],[0,1.67,.235],[-.14,1.76,.19]],'whiteLight');
panel('white_collar_right','body',[[.22,1.63,.22],[.03,1.48,.252],[0,1.67,.235],[.14,1.76,.19]],'whiteLight');
panel('blue_necktie','body',[[-.054,1.56,.277],[.054,1.56,.277],[.075,1.28,.279],[0,1.20,.286],[-.075,1.28,.279]],'navyLight');
jewel('chest_brooch','body',[0,1.56,.299],.043,'goldLight');
for(const s of [-1,1]) panel(`skirt_gold_flourish_${s}`,'body',[[s*.12,.78,.42],[s*.21,.85,.39],[s*.24,.95,.352],[s*.17,.91,.39]],'goldLight');

// Puffy sleeves taper into patterned long cuffs and white gloves.
for(const s of [-1,1]) {
  const bone=s<0?'right_arm':'left_arm';
  rings(`${bone}_puff`,bone,[[s*.34,1.49,0,.16,.17],[s*.43,1.37,.02,.19,.18],[s*.45,1.23,.035,.14,.145]],'white',10,['white','whiteLight','blueWhite','whiteShade']);
  rings(`${bone}_sleeve`,bone,[[s*.45,1.25,.035,.13,.14],[s*.49,.84,.07,.105,.116]],'white',10,['white','blueWhite','white','whiteShade']);
  for(let i=0;i<4;i++) {
    const y=1.15-i*.095,x=s*(.462+i*.008);
    jewel(`${bone}_blue_diamond_${i}`,bone,[x,y,.183],.034,i%2?'blueLight':'blue');
  }
  rings(`${bone}_gold_cuff`,bone,[[s*.49,.87,.07,.111,.121],[s*.505,.805,.073,.108,.119]],'gold',10,['gold','goldLight','gold','goldShade']);
  rings(`${bone}_glove`,bone,[[s*.505,.81,.073,.106,.116],[s*.52,.68,.083,.095,.105]],'whiteLight',10,['white','whiteLight','white','whiteShade']);
}

// Soft faceted face with half-lidded rose eyes.
function headRing(y,w,d,z=.02){return [[-w*.72,y,z+d],[w*.72,y,z+d],[w,y,z+d*.66],[w,y,z-d*.62],[w*.72,y,z-d],[-w*.72,y,z-d],[-w,y,z-d*.62],[-w,y,z+d*.66]];}
const hv=[...headRing(1.61,.225,.222),...headRing(1.67,.292,.25),...headRing(2.07,.319,.258),...headRing(2.145,.267,.22,.005)],hf=[];
for(let j=0;j<3;j++)for(let i=0;i<8;i++)hf.push([j*8+i,j*8+(i+1)%8,(j+1)*8+(i+1)%8,(j+1)*8+i]);
hf.push([24,25,26,27],[24,27,28,29],[24,29,30,31],[0,3,2,1],[0,5,4,3],[0,7,6,5]);mesh('soft_face','head',hv,hf,'skin');
for(const s of [-1,1]) {
  const x=s*.139;
  panel(`eye_white_${s}`,'head',[[x-.086,1.853,.283],[x+.086,1.853,.283],[x+.074,1.792,.288],[x-.069,1.79,.288]],'whiteLight');
  box(`iris_${s}`,'head',[x,1.817,.3],[.07,.07,.009],'eye');
  box(`iris_lower_${s}`,'head',[x,1.795,.307],[.057,.022,.009],'eyeLight');
  box(`pupil_${s}`,'head',[x,1.824,.312],[.019,.044,.007],'eyeShade');
  panel(`sleepy_lash_${s}`,'head',[[x-.092,1.875,.307],[x+.09,1.868,.307],[x+.104,1.846,.308],[x-.086,1.852,.308]],'ink');
  box(`eyebrow_${s}`,'head',[x,1.925,.288],[.105,.012,.01],'hairShade',s*.04);
  box(`blush_${s}`,'head',[s*.225,1.731,.279],[.053,.013,.006],'blush');
}
box('small_mouth','head',[0,1.683,.269],[.041,.008,.008],'mouth');

// Golden hair cap, parted fringe and very long side/back locks.
rings('hair_cap','hair',[[0,1.72,-.045,.325,.265],[0,2.08,-.035,.354,.295],[0,2.205,-.052,.286,.245]],'hair',12,['hair','hairLight','hair','hairShade']);
for(const [i,x] of [-.245,-.12,.015,.14,.25].entries()) {
  const end=i===1?1.80:i===2?1.89:1.92;
  lock(`fringe_${i}`,'hair',[[x,2.16,.282,.073,.045],[x*.98,2.04,.318,.079,.052],[x*.91,end,.303,.009,.016]]);
}
for(const s of [-1,1]) for(let i=0;i<4;i++) {
  const outer=i*.045;
  lock(`long_hair_${s}_${i}`,'hair',[[s*(.24+outer),2.08,.12-i*.065,.071,.076],[s*(.31+outer),1.72,.10-i*.057,.074,.075],[s*(.34+outer*.8),1.20,.07-i*.052,.069,.07],[s*(.29+outer*.55),.74,.10-i*.045,.058,.059],[s*(.23+outer*.4),.55,.15-i*.03,.009,.014]]);
}
for(let i=0;i<7;i++) lock(`back_hair_${i}`,'hair',[[(-.24+i*.08),2.08,-.18,.065,.06],[(-.29+i*.097),1.58,-.25,.076,.069],[(-.27+i*.09),1.02,-.28,.068,.062],[(-.22+i*.073),.62,-.24,.012,.015]]);

// Tall, unmistakably tapered fox ears with cream inner fur and small flowers.
for(const s of [-1,1]) {
  panel(`${s<0?'left':'right'}_fox_ear`,'head',[[s*.10,2.18,.025],[s*.37,2.17,.005],[s*.35,2.64,-.055]],'ear',.15);
  panel(`${s<0?'left':'right'}_fox_ear_inner`,'head',[[s*.185,2.225,.101],[s*.318,2.22,.087],[s*.326,2.51,-.006]],'earInner');
  panel(`fox_ear_inner_shadow_${s}`,'head',[[s*.232,2.26,.111],[s*.303,2.255,.101],[s*.316,2.43,.039]],'hairLight');
  for(let i=0;i<3;i++) {
    const x=s*(.18+i*.052),y=2.19+i*.015;
    for(let p=0;p<5;p++){const a=p*2*Math.PI/5;panel(`ear_flower_${s}_${i}_${p}`,'head',Array.from({length:5},(_,k)=>[x+Math.cos(a)*.025+Math.cos(k*2*Math.PI/5)*.018,y+Math.sin(a)*.021+Math.sin(k*2*Math.PI/5)*.014,.164]),'whiteLight',.009);}
    jewel(`ear_flower_center_${s}_${i}`,'head',[x,y,.18],.012,'goldLight');
  }
}

// A large articulated fox tail curls beside the dress; the light tip remains visible in back views.
tube('large_fox_tail','tail',[[0,1.02,-.25,.19],[.28,.92,-.42,.23],[.55,.70,-.52,.25],[.72,.45,-.42,.23],[.72,.24,-.16,.18],[.53,.18,.09,.14],[.29,.22,.24,.075],[.12,.30,.29,.015]],['hairShade','hair','hair','hairLight','hairLight','tailTip','tailTip'],12);
for(let i=0;i<3;i++) panel(`tail_ribbon_${i}`,'tail',[[.26+i*.035,.22+i*.02,.27],[.36+i*.03,.31+i*.018,.22],[.28+i*.028,.38+i*.018,.18],[.19+i*.03,.30+i*.015,.23]],i===1?'blueLight':'whiteLight',.025);

// Tiny blue-white bird from the reference, perched close to the right shoulder.
rings('shoulder_bird','right_arm',[[-.37,1.65,.14,.065,.073],[-.37,1.78,.14,.052,.058]],'bird',8,['bird','whiteLight','birdBlue','bird']);
panel('bird_left_wing','right_arm',[[-.38,1.72,.19],[-.49,1.67,.15],[-.42,1.78,.15]],'birdBlue');
panel('bird_right_wing','right_arm',[[-.35,1.72,.19],[-.25,1.67,.15],[-.31,1.78,.15]],'birdBlue');
panel('bird_beak','right_arm',[[-.395,1.793,.187],[-.345,1.793,.187],[-.37,1.765,.187]],'beak');
box('bird_eye','right_arm',[-.388,1.806,.192],[.012,.012,.006],'navy');

// Ornate tilted halo: double ring, three crown crescents and four small diamonds.
const hy=2.72,seg=28;
for(let i=0;i<seg;i++) {
  const a=i*2*Math.PI/seg,b=(i+1)*2*Math.PI/seg;
  mesh(`halo_ring_${i}`,'halo',[[Math.sin(a)*.25,hy,Math.cos(a)*.25],[Math.sin(b)*.25,hy,Math.cos(b)*.25],[Math.sin(b)*.292,hy,Math.cos(b)*.292],[Math.sin(a)*.292,hy,Math.cos(a)*.292],[Math.sin(a)*.25,hy+.022,Math.cos(a)*.25],[Math.sin(b)*.25,hy+.022,Math.cos(b)*.25],[Math.sin(b)*.292,hy+.022,Math.cos(b)*.292],[Math.sin(a)*.292,hy+.022,Math.cos(a)*.292]],[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]],i%3?'halo':'haloLight');
}
for(let i=0;i<3;i++) {
  const a=(i-1)*.82;
  const rotate=([x,y,z])=>[x*Math.cos(a)+z*Math.sin(a),y,-x*Math.sin(a)+z*Math.cos(a)];
  mesh(`halo_crown_${i}`,'halo',[[0,hy,.48],[-.105,hy,.265],[0,hy,.32],[.105,hy,.265],[0,hy+.045,.34]].map(rotate),[[0,1,4],[1,2,4],[2,3,4],[3,0,4],[0,3,2,1]],'haloLight');
}
for(let i=0;i<4;i++){const a=i*Math.PI/2;jewel(`halo_diamond_${i}`,'halo',[Math.sin(a)*.17,hy+.04,Math.cos(a)*.17],.045,i%2?'goldLight':'haloLight');}
for(const o of objects.filter(o=>o.bone==='halo')) o.vertices=o.vertices.map(([x,y,z])=>[x,+(hy+(y-hy)*Math.cos(.36)+z*Math.sin(.36)).toFixed(5),+(-(y-hy)*Math.sin(.36)+z*Math.cos(.36)).toFixed(5)]);

fs.mkdirSync(output,{recursive:true});
const data={format:2,texture:'capitalcraft:textures/entity/resident/seia.png',unitsPerBlock:16,modelScale:12,bones,palette,objects};
fs.writeFileSync(path.join(output,'seia-mesh.json'),JSON.stringify(data)+'\n');
const obj=['# Same geometry as seia-mesh.json; Y up, +Z front; 0.75 blocks per unit.','mtllib seia-resident.mtl'];let index=1;
for(const o of objects){obj.push(`o ${o.name}`);for(const p of o.vertices)obj.push(`v ${p.map(v=>(v*.75).toFixed(6)).join(' ')}`);for(const f of o.faces){obj.push(`usemtl ${f.material}`);obj.push(`f ${f.indices.map(i=>index+i).join(' ')}`);}index+=o.vertices.length;}
fs.writeFileSync(path.join(output,'seia-resident.obj'),obj.join('\n')+'\n');
const rgb=hex=>[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16));
fs.writeFileSync(path.join(output,'seia-resident.mtl'),Object.entries(palette).map(([k,c])=>`newmtl ${k}\nKd ${rgb(c).map(v=>(v/255).toFixed(6)).join(' ')}\nd 1\nillum 1`).join('\n\n')+'\n');
const pixels=Buffer.alloc(64*64*4),colors=Object.values(palette).map(rgb);
for(let y=0;y<64;y++)for(let x=0;x<64;x++){const color=colors[Math.floor(y/8)*8+Math.floor(x/8)]??colors[0],p=(y*64+x)*4;pixels.set([...color,255],p);}
writePng(path.join(base,'src/main/resources/assets/capitalcraft/textures/entity/resident/seia.png'),64,64,pixels);
const ys=objects.flatMap(o=>o.vertices.map(p=>p[1]));
const summary={format:'shared-mesh-v2',objectCount:objects.length,vertexCount:index-1,faceCount:objects.reduce((n,o)=>n+o.faces.length,0),heightBlocks:+((Math.max(...ys)-Math.min(...ys))*.75).toFixed(4),foxEarParts:['left_fox_ear','right_fox_ear','left_fox_ear_inner','right_fox_ear_inner'],signatureParts:['large_fox_tail','shoulder_bird','halo_crown_0'],runtimeMesh:'seia-mesh.json',preview:'seia-preview.png'};
fs.writeFileSync(path.join(output,'seia-resident-model.json'),JSON.stringify(summary,null,2)+'\n');console.log(summary);
