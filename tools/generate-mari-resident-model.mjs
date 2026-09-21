// One mesh source for the in-game ModelParts, editable OBJ, and software preview.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { writePng } from './mari-png.mjs';

const base = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const output = path.join(base, 'src/main/resources/assets/capitalcraft/models/entity/resident/mari');
const palette = {
  cloth: '#35343e', clothLight: '#42414d', clothDark: '#2a2932', veil: '#303039', veilLight: '#3b3b46',
  white: '#eee8e7', whiteLight: '#fff9f4', whiteShade: '#d9d0d4', skin: '#ffe1cf', skinShade: '#f4c9b9',
  blush: '#efb2a6', mouth: '#d9a095', hair: '#f5ac83', hairLight: '#ffc69c', hairShade: '#df8e74',
  gold: '#c6ac69', goldLight: '#e4cb86', goldShade: '#a78b51', teal: '#709fa4', tealLight: '#8bb6ba',
  tealShade: '#56878d', eye: '#71c5ce', eyeLight: '#b9edf1', eyeShade: '#468c9b', ink: '#48404c',
  ear: '#b4a2ad', earLight: '#ded0d2', halo: '#efd080', haloLight: '#ffe6a5', leaf: '#568b85',
  flower: '#dbeef3', flowerShade: '#9cc9d5', shoe: '#333139', sole: '#ac945b'
};
const bones = {
  body: [0, 0, 0], head: [0, 1.63, 0], veil: [0, 1.59, 0],
  left_arm: [0.34, 1.49, 0], right_arm: [-0.34, 1.49, 0],
  left_leg: [0.15, 0.48, 0], right_leg: [-0.15, 0.48, 0], halo: [0, 2.73, 0]
};
const objects = [];
const sub = (a,b) => a.map((v,i)=>v-b[i]);
const cross = (a,b) => [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const dot = (a,b) => a.reduce((s,v,i)=>s+v*b[i],0);
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
function rings(name,bone,sections,material,sides=8,shades=[],offset=0) {
  const vertices=sections.flatMap(([x,y,z,rx,rz])=>Array.from({length:sides},(_,i)=>{
    const a=2*Math.PI*i/sides+offset;
    return [x+Math.sin(a)*rx,y,z+Math.cos(a)*rz];
  }));
  const faces=[];
  for(let j=0;j<sections.length-1;j++) for(let i=0;i<sides;i++) {
    const next=(i+1)%sides;
    faces.push([j*sides+i,j*sides+next,(j+1)*sides+next,(j+1)*sides+i]);
  }
  for (const j of [0,sections.length-1]) for(let i=1;i<sides-1;i++) faces.push([j*sides,j*sides+i,j*sides+i+1]);
  mesh(name,bone,vertices,faces,material,shades);
}
function box(name,bone,[x,y,z],[w,h,d],mat,rz=0) {
  const points=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]];
  const vertices=points.map(([a,b,c])=>[x+a*w/2*Math.cos(rz)-b*h/2*Math.sin(rz),y+a*w/2*Math.sin(rz)+b*h/2*Math.cos(rz),z+c*d/2]);
  mesh(name,bone,vertices,[[0,3,2,1],[4,5,6,7],[0,4,7,3],[1,2,6,5],[3,7,6,2],[0,1,5,4]],mat);
}
function panel(name,bone,points,mat,depth=.014) {
  const vertices=[...points, ...points.map(([x,y,z])=>[x,y,z-depth])];
  const n=points.length,faces=[];
  for(let i=1;i<n-1;i++){faces.push([0,i,i+1]); faces.push([n,n+i+1,n+i]);}
  for(let i=0;i<n;i++) faces.push([i,(i+1)%n,(i+1)%n+n,i+n]);
  mesh(name,bone,vertices,faces,mat);
}
function lock(name,sections,bone='head') {
  rings(name,bone,sections,'hair',8,['hair','hairLight','hair','hairShade','hairShade','hair','hairLight','hair']);
}
function jewel(name,bone,[x,y,z],r,mat='goldLight') {
  mesh(name,bone,[[x-r,y,z],[x,y+r,z],[x+r,y,z],[x,y-r,z],[x,y,z+.035]],[[0,1,4],[1,2,4],[2,3,4],[3,0,4],[0,3,2,1]],mat);
}

// Rounded Mary Jane shoes and visible socks, in separate walking bones.
for (const s of [-1,1]) {
  const bone=s<0?'right_leg':'left_leg', x=s*.15;
  rings(`${bone}_sole`,bone,[[x,0,.06,.122,.19],[x,.035,.06,.125,.195]],'sole',8);
  rings(`${bone}_shoe`,bone,[[x,.025,.06,.124,.19],[x,.125,.055,.118,.18],[x,.175,.005,.084,.11]],'shoe',8);
  box(`${bone}_sock`,bone,[x,.285,-.005],[.16,.28,.16],'white');
  box(`${bone}_ankle_trim`,bone,[x,.31,0],[.174,.043,.175],'gold');
  box(`${bone}_instep`,bone,[x,.154,.086],[.142,.018,.135],'white');
  box(`${bone}_strap`,bone,[x,.167,.075],[.17,.021,.036],'shoe');
}

// Bell-shaped, faceted dress. The front apron follows the skirt, without floating.
rings('pleated_white_hem','body',[[0,.34,0,.49,.32],[0,.49,0,.468,.308]],'white',16,['white','whiteShade','white','whiteLight']);
rings('flared_black_skirt','body',[[0,.49,0,.469,.31],[0,.85,0,.349,.246],[0,1.13,0,.237,.17]],'cloth',16,['cloth','clothLight','cloth','clothDark']);
rings('fitted_bodice','body',[[0,1.08,0,.243,.175],[0,1.35,0,.255,.18],[0,1.5,0,.285,.17]],'cloth',8);
// The apron follows every skirt ring; a single straight panel would cut into it.
const apronLevels=[[.365,.49,.32],[.49,.469,.31],[.85,.349,.246],[1.13,.243,.177],[1.17,.243,.182]];
for(let i=0;i<3;i++) for(let j=0;j<apronLevels.length-1;j++) {
  const a=(-30+20*i)*Math.PI/180,b=(-10+20*i)*Math.PI/180;
  const p=([y,rx,rz],t)=>[Math.sin(t)*rx,y,Math.cos(t)*rz+.018];
  panel(`apron_pleat_${i}_${j}`,'body',[p(apronLevels[j],a),p(apronLevels[j],b),p(apronLevels[j+1],b),p(apronLevels[j+1],a)],i===1?'whiteLight':'white');
}
for(let i=0;i<16;i++) {
  const a=2*Math.PI*(i+.5)/16;
  if(Math.abs(Math.sin(a))<.46 && Math.cos(a)>0) continue;
  const x=Math.sin(a)*.472,z=Math.cos(a)*.313;
  box(`hem_gold_tab_${i}`,'body',[x,.43,z],[.043,.138,.025],'gold');
}
box('neck','body',[0,1.555,.015],[.145,.15,.14],'skin');
panel('white_collar','body',[[-.18,1.578,.165],[.18,1.578,.165],[.23,1.5,.184],[.135,1.327,.209],[0,1.277,.216],[-.135,1.327,.209],[-.23,1.5,.184]],'whiteLight');
panel('collar_dark_inset','body',[[-.085,1.503,.201],[.085,1.503,.201],[0,1.392,.222]],'ink');
for(const s of [-1,1]) panel(`collar_edge_${s}`,'body',[[s*.217,1.48,.206],[s*.128,1.33,.227],[0,1.283,.235],[s*.129,1.315,.227],[s*.237,1.48,.206]],'ink');
jewel('collar_cross_center','body',[0,1.364,.245],.018,'ink');
for(const [x,y] of [[-.03,1.364],[.03,1.364],[0,1.394],[0,1.334]]) jewel(`collar_cross_${x}_${y}`,'body',[x,y,.245],.009,'ink');
for(const s of [-1,1]) {
  panel(`teal_bow_loop_${s}`,'body',[[0,1.255,.263],[s*.245,1.342,.216],[s*.186,1.173,.245]],'teal');
  panel(`teal_ribbon_${s}`,'body',[[s*.015,1.255,.275],[s*.102,1.239,.27],[s*.144,1.052,.279],[s*.059,1.017,.29]],s<0?'tealLight':'teal');
}
panel('teal_center_tail','body',[[-.035,1.235,.28],[.035,1.235,.28],[.068,1.018,.30],[0,.958,.313],[-.068,1.018,.30]],'tealShade');
jewel('chest_brooch','body',[0,1.245,.3],.052);
for(const [x,y] of [[-.018,1.245],[.018,1.245],[0,1.263],[0,1.227]]) jewel(`brooch_facet_${x}_${y}`,'body',[x,y,.338],.013,'gold');

// A relaxed arm pose, embroidered shoulder bands, broad sleeves, cuffs and gloves.
for(const s of [-1,1]) {
  const b=s<0?'right_arm':'left_arm';
  rings(`${b}_sleeve`,b,[[s*.31,1.49,0,.1,.145],[s*.355,1.25,.015,.13,.15],[s*.455,.96,.035,.163,.165]],'cloth',8,['cloth','clothLight','cloth','clothDark']);
  rings(`${b}_embroidered_band`,b,[[s*.325,1.407,.012,.122,.155],[s*.351,1.275,.018,.135,.16]],'gold',8,['gold','goldLight','gold','goldShade']);
  for(let row=0;row<3;row++) for(let col=0;col<4;col++) if((row+col)%2===0) {
    box(`${b}_embroidery_${row}_${col}`,b,[s*.338+(col-1.5)*.043,1.385-row*.042,.174],[.029,.032,.009],'clothDark');
  }
  rings(`${b}_cuff`,b,[[s*.458,.961,.031,.106,.126],[s*.492,.84,.055,.102,.123]],'gold',8,['gold','goldLight','gold','goldShade']);
  for(let i=0;i<3;i++) {
    const t=.2+i*.27,y=.961-t*.121,x=s*(.458+t*.034),z=.031+t*.024;
    rings(`${b}_cuff_engraving_${i}`,b,[[x,y,z,.107-t*.004,.127-t*.003],[x,y-.004,z,.107-t*.004,.127-t*.003]],'goldShade',8);
  }
  rings(`${b}_glove`,b,[[s*.492,.85,.055,.107,.123],[s*.522,.731,.069,.103,.117],[s*.52,.7,.066,.083,.095]],'white',8,['white','whiteLight','white','whiteShade']);
  for(let i=0;i<3;i++) box(`${b}_glove_seam_${i}`,b,[s*.52+(i-1)*.041,.728,.173],[.005,.038,.005],'whiteShade');
}

// Gold sash and the large rear bow visible in the reference's back view.
rings('gold_back_sash','body',[[0,1.14,-.015,.246,.188],[0,1.23,-.015,.249,.19]],'gold',12);
// Front apron/bow sit in front of the sash; the rear bow is built facing backwards.
for(const s of [-1,1]) {
  panel(`back_bow_loop_${s}`,'body',[[0,1.1,-.24],[s*.18,1.145,-.218],[s*.195,.986,-.245],[0,1.02,-.28]],'gold',-.025);
  panel(`back_bow_tail_${s}`,'body',[[s*.025,1.05,-.255],[s*.10,1.04,-.26],[s*.24,.63,-.326],[s*.114,.70,-.34]],s<0?'gold':'goldLight',-.016);
}
box('back_bow_knot','body',[0,1.054,-.275],[.107,.107,.074],'goldLight',Math.PI/4);

// Chamfered face: flat front, rounded sides, tapered jaw (not a giant cube).
function headRing(y,w,d,z=0) {
  return [[-w*.73,y,z+d],[w*.73,y,z+d],[w,y,z+d*.67],[w,y,z-d*.62],[w*.72,y,z-d],[-w*.72,y,z-d],[-w,y,z-d*.62],[-w,y,z+d*.67]];
}
const hv=[...headRing(1.61,.232,.23,.025),...headRing(1.67,.3,.255,.025),...headRing(2.075,.326,.261,.02),...headRing(2.145,.272,.223,.005)];
const hf=[]; for(let j=0;j<3;j++)for(let i=0;i<8;i++)hf.push([j*8+i,j*8+(i+1)%8,(j+1)*8+(i+1)%8,(j+1)*8+i]);
hf.push([24,25,26,27],[24,27,28,29],[24,29,30,31],[0,3,2,1],[0,5,4,3],[0,7,6,5]);
mesh('soft_face','head',hv,hf,'skin');
for(const s of [-1,1]) {
  const x=s*.143;
  panel(`eye_white_${s}`,'head',[[x-.084,1.86,.287],[x+.084,1.86,.287],[x+.079,1.776,.287],[x-.079,1.776,.287]],'whiteLight');
  box(`iris_${s}`,'head',[x,1.815,.3],[.077,.094,.009],'eye');
  box(`iris_top_${s}`,'head',[x,1.845,.307],[.077,.027,.009],'eyeShade');
  box(`iris_bottom_${s}`,'head',[x,1.78,.31],[.063,.017,.009],'eyeLight');
  box(`pupil_${s}`,'head',[x,1.824,.313],[.023,.058,.007],'eyeShade');
  box(`catchlight_${s}`,'head',[x-.018,1.843,.32],[.024,.024,.006],'whiteLight');
  panel(`eyelash_${s}`,'head',[[x-.086,1.881,.308],[x+.085,1.876,.308],[x+.095,1.848,.309],[x-.086,1.854,.309]],'ink');
  box(`eyebrow_${s}`,'head',[x,1.912,.29],[.11,.013,.011],'hairShade',s*.07);
  box(`blush_${s}`,'head',[s*.231,1.727,.286],[.048,.013,.006],'blush');
}
box('small_mouth','head',[0,1.672,.27],[.043,.008,.008],'mouth');
jewel('nose','head',[0,1.749,.295],.012,'skinShade');

// Hair cap, individual pointed fringe locks, side tresses and a woven braid.
rings('hair_cap','head',[[0,1.74,-.05,.336,.27],[0,2.10,-.03,.365,.305],[0,2.22,-.04,.29,.25]],'hair',12);
for(const [i,x] of [-.244,-.124,0,.124,.244].entries()) {
  const center=i===2, bottom=center?1.863:1.913+(i===0||i===4?.02:0);
  lock(`fringe_${i}`,[[x,2.17,.276,.077,.045],[x*1.04,2.064,.313,.085,.055],[x*.96,1.985,.328,.067,.05],[x*.91,bottom,.304,.009,.019]]);
}
for(const s of [-1,1]) {
  for(let i=0;i<3;i++) lock(`side_hair_${s}_${i}`,[[s*(.268+i*.046),2.12,.175-i*.077,.061,.075],[s*(.32+i*.04),1.91,.184-i*.07,.065,.069],[s*(.307+i*.034),1.67,.204-i*.06,.062,.067],[s*(.265+i*.03),1.49,.236-i*.045,.013,.022]]);
  if(s>0) lock('long_side_tress',[[.32,1.81,.221,.063,.055],[.283,1.57,.246,.057,.046],[.251,1.31,.254,.041,.036],[.229,1.2,.269,.006,.008]]);
}
for(let i=0;i<6;i++) for(const s of [-1,1]) {
  const y=1.552-i*.067,x=-.285+s*.023;
  lock(`braid_${i}_${s}`,[[x-s*.014,y+.053,.263,.031,.038],[x+s*.019,y+.015,.28,.041,.04],[x-s*.007,y-.04,.27,.019,.026]]);
}
box('braid_gold_tie','head',[-.286,1.153,.279],[.107,.045,.084],'gold');
lock('braid_tassel',[[-.286,1.136,.276,.046,.04],[-.29,1.055,.28,.05,.037],[-.302,.981,.283,.007,.01]]);

// A faceted hood and long back veil with open face. Broad back panels hide hair.
rings('veil_crown','head',[[0,2.101,-.062,.399,.335],[0,2.249,-.061,.392,.323],[0,2.292,-.075,.303,.25]],'veil',12,['veil','veilLight','veil','clothDark']);
const veilSections=[[2.22,.398,.35],[1.82,.412,.354],[1.35,.478,.386],[1.079,.491,.4]];
const vv=[];const angles=[62,88,118,148,180,212,242,272,298].map(v=>v*Math.PI/180);
for (const [y,rx,rz] of veilSections) for(const a of angles) vv.push([Math.sin(a)*rx,y+(y<1.1?.15*Math.max(0,-Math.cos(a)):0),Math.cos(a)*rz-.018]);
const vf=[];for(let j=0;j<3;j++)for(let i=0;i<angles.length-1;i++)vf.push([j*9+i,j*9+i+1,(j+1)*9+i+1,(j+1)*9+i]);
mesh('long_back_veil','veil',vv,vf,'veil',['veil','veilLight','veil','clothDark','veil','veilLight','veil','veilLight']);
for(const s of [-1,1]) {
  panel(`white_veil_lining_${s}`,'veil',[[s*.346,1.97,.152],[s*.4,1.85,.164],[s*.564,1.025,.03],[s*.423,1.075,.06]],'white');
  panel(`veil_outer_edge_${s}`,'veil',[[s*.392,2.147,.174],[s*.414,1.908,.197],[s*.591,1.032,.047],[s*.56,1.014,.052]],'veil');
  for(const [dx,dy] of [[0,0],[-.027,0],[.027,0],[0,.034],[0,-.034]]) box(`veil_cross_${s}_${dx}_${dy}`,'veil',[s*.507+dx,1.105+dy,.093],[.024,.024,.009],'gold');
}
// Distinctly tapered FOX ears, solid triangular prisms with an inset, no stepped rods.
for(const s of [-1,1]) {
  panel(`${s<0?'left':'right'}_fox_ear`,'head',[[s*.103,2.219,.05],[s*.403,2.157,.04],[s*.439,2.527,-.065]],'veil',.16);
  panel(`${s<0?'left':'right'}_fox_ear_inner`,'head',[[s*.27,2.223,.072],[s*.373,2.216,.074],[s*.406,2.427,-.001]],'ear');
  panel(`fox_ear_inner_highlight_${s}`,'head',[[s*.324,2.236,.081],[s*.371,2.228,.08],[s*.397,2.377,.027]],'earLight');
}
// Gold band, following the rounded forehead and side of the hood.
for(let i=0;i<8;i++) {
  const a=(-78+i*19.5)*Math.PI/180,b=(-78+(i+1)*19.5)*Math.PI/180;
  panel(`forehead_band_${i}`,'head',[[Math.sin(a)*.392,2.09,Math.cos(a)*.402],[Math.sin(b)*.392,2.09,Math.cos(b)*.402],[Math.sin(b)*.392,2.204,Math.cos(b)*.402],[Math.sin(a)*.392,2.204,Math.cos(a)*.402]],i%3===0?'goldLight':'gold');
}

// Three blue-white flowers with gold centres, leaves and buds at the left temple.
for(const [i,x,y,r] of [[0,-.344,2.1,.037],[1,-.29,2.046,.031],[2,-.373,2.174,.024]]) {
  for(let j=0;j<5;j++) {
    const a=j*2*Math.PI/5;
    const px=x+Math.cos(a)*r,py=y+Math.sin(a)*r;
    panel(`flower_${i}_petal_${j}`,'head',Array.from({length:6},(_,k)=>[px+Math.cos(k*Math.PI/3)*r*.65,py+Math.sin(k*Math.PI/3)*r*.75,.404]),j%2?'flower':'whiteLight',.012);
  }
  jewel(`flower_${i}_center`,'head',[x,y,.428],r*.37,'goldLight');
}
for (const [i,x,y] of [[0,-.412,2.103],[1,-.329,2.197],[2,-.372,1.996]]) panel(`flower_leaf_${i}`,'head',[[x-.057,y,.34],[x,y+.032,.36],[x+.039,y,.345],[x,y-.037,.347]],'leaf');
// Follow the rounded band at the temple so flowers do not float ahead of the hood.
for(const o of objects.filter(o=>o.name.startsWith('flower_'))) o.vertices=o.vertices.map(([x,y,z])=>[x,y,+(z+Math.sqrt(Math.max(.08,1-(x/.435)**2))*.4-.335).toFixed(5)]);

// Horizontal star-and-ring halo, with four tips, cross spokes and a centre diamond.
const hy=2.712,seg=24;
for(let i=0;i<seg;i++) {
  const a=i*2*Math.PI/seg,b=(i+1)*2*Math.PI/seg;
  mesh(`halo_ring_${i}`,'halo',[[Math.sin(a)*.263,hy,Math.cos(a)*.263],[Math.sin(b)*.263,hy,Math.cos(b)*.263],[Math.sin(b)*.305,hy,Math.cos(b)*.305],[Math.sin(a)*.305,hy,Math.cos(a)*.305],[Math.sin(a)*.263,hy+.023,Math.cos(a)*.263],[Math.sin(b)*.263,hy+.023,Math.cos(b)*.263],[Math.sin(b)*.305,hy+.023,Math.cos(b)*.305],[Math.sin(a)*.305,hy+.023,Math.cos(a)*.305]],[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]],i%3?'halo':'haloLight');
}
box('halo_cross_x','halo',[0,hy+.012,0],[.51,.016,.028],'haloLight');
box('halo_cross_z','halo',[0,hy+.012,0],[.028,.016,.51],'haloLight');
for(let i=0;i<4;i++) {
  const a=i*Math.PI/2,rot=([x,y,z])=>[x*Math.cos(a)+z*Math.sin(a),y,-x*Math.sin(a)+z*Math.cos(a)];
  mesh(`halo_star_tip_${i}`,'halo',[[0,hy,.409],[-.067,hy,.281],[0,hy,.207],[.067,hy,.281],[0,hy+.04,.282]].map(rot),[[0,1,4],[1,2,4],[2,3,4],[3,0,4],[0,3,2,1]],'haloLight');
}
mesh('halo_center_gem','halo',[[0,hy,.081],[-.081,hy,0],[0,hy,-.081],[.081,hy,0],[0,hy+.062,0],[0,hy-.02,0]],[[0,1,4],[1,2,4],[2,3,4],[3,0,4],[0,5,1],[1,5,2],[2,5,3],[3,5,0]],'halo');
// A slight tilt keeps the star motif readable from the ordinary player viewpoint.
for(const o of objects.filter(o=>o.bone==='halo')) o.vertices=o.vertices.map(([x,y,z])=>[x,+(hy+(y-hy)*Math.cos(.40)+z*Math.sin(.40)).toFixed(5),+(-(y-hy)*Math.sin(.40)+z*Math.cos(.40)).toFixed(5)]);

fs.mkdirSync(output,{recursive:true});
const data={format:2,texture:'capitalcraft:textures/entity/resident/mari.png',unitsPerBlock:16,modelScale:12,bones,palette,objects};
fs.writeFileSync(path.join(output,'mari-mesh.json'),JSON.stringify(data)+'\n');
const obj=['# Same geometry as mari-mesh.json; Y up, +Z front; 0.75 blocks per unit.','mtllib mari-resident.mtl'];
let index=1;
for(const o of objects) {
  obj.push(`o ${o.name}`);
  for(const p of o.vertices)obj.push(`v ${p.map(v=>(v*.75).toFixed(6)).join(' ')}`);
  for(const f of o.faces) {obj.push(`usemtl ${f.material}`);obj.push(`f ${f.indices.map(i=>index+i).join(' ')}`);}
  index+=o.vertices.length;
}
fs.writeFileSync(path.join(output,'mari-resident.obj'),obj.join('\n')+'\n');
const rgb=hex=>[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16));
fs.writeFileSync(path.join(output,'mari-resident.mtl'),Object.entries(palette).map(([k,c])=>`newmtl ${k}\nKd ${rgb(c).map(v=>(v/255).toFixed(6)).join(' ')}\nd 1\nillum 1`).join('\n\n')+'\n');
const pixels=Buffer.alloc(64*64*4), colors=Object.values(palette).map(rgb);
for(let y=0;y<64;y++)for(let x=0;x<64;x++) {
  const color=colors[Math.floor(y/8)*8+Math.floor(x/8)]??colors[0],p=(y*64+x)*4;
  pixels.set([...color,255],p);
}
writePng(path.join(base,'src/main/resources/assets/capitalcraft/textures/entity/resident/mari.png'),64,64,pixels);
const ys=objects.flatMap(o=>o.vertices.map(p=>p[1]));
const summary={format:'shared-mesh-v2',objectCount:objects.length,vertexCount:index-1,faceCount:objects.reduce((n,o)=>n+o.faces.length,0),heightBlocks:+((Math.max(...ys)-Math.min(...ys))*.75).toFixed(4),foxEarParts:['left_fox_ear','right_fox_ear','left_fox_ear_inner','right_fox_ear_inner'],runtimeMesh:'mari-mesh.json',preview:'mari-preview.png'};
fs.writeFileSync(path.join(output,'mari-resident-model.json'),JSON.stringify(summary,null,2)+'\n');
console.log(summary);
