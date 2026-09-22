// Close views of the actual mesh: Mari above, Seia below; front, 3/4, side.
// Optional baseline directory contains mari-before.json and seia-before.json.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {writePng} from './mari-png.mjs';

const base=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const output=process.argv[2]??path.join(base,'build/resident-heads.png');
const baseline=process.argv[3];
const width=1800,height=1500,rgba=Buffer.alloc(width*height*4);
const depth=new Float64Array(width*height).fill(-Infinity);
for(let y=0;y<height;y++)for(let x=0;x<width;x++)rgba.set([229,233,237,255],(y*width+x)*4);
const sub=(a,b)=>a.map((v,i)=>v-b[i]);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
function triangle(points,color,column,row) {
  const [a,b,c]=points;
  const den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);
  if(Math.abs(den)<1e-8)return;
  const xmin=Math.max(column*600,Math.floor(Math.min(a[0],b[0],c[0])));
  const xmax=Math.min(column*600+599,Math.ceil(Math.max(a[0],b[0],c[0])));
  const ymin=Math.max(row*750,Math.floor(Math.min(a[1],b[1],c[1])));
  const ymax=Math.min(row*750+749,Math.ceil(Math.max(a[1],b[1],c[1])));
  for(let y=ymin;y<=ymax;y++)for(let x=xmin;x<=xmax;x++) {
    const u=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den;
    const v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,w=1-u-v;
    if(Math.min(u,v,w)<0)continue;
    const z=u*a[2]+v*b[2]+w*c[2],i=y*width+x;
    if(z<depth[i])continue;
    depth[i]=z;rgba.set([...color,255],i*4);
  }
}
for(const [row,name] of ['mari','seia'].entries()) {
  const file=baseline?path.join(baseline,`${name}-before.json`):
    path.join(base,`src/main/resources/assets/capitalcraft/models/entity/resident/${name}/${name}-mesh.json`);
  const model=JSON.parse(fs.readFileSync(file,'utf8'));
  for(const [column,yaw] of [0,Math.PI/4,-Math.PI/2].entries()) {
    const pitch=.14,scale=560;
    const rotate=([x,y,z])=>{
      const xx=x*Math.cos(yaw)+z*Math.sin(yaw),zz=-x*Math.sin(yaw)+z*Math.cos(yaw);
      return [xx,(y-2.18)*Math.cos(pitch)-zz*Math.sin(pitch),(y-2.18)*Math.sin(pitch)+zz*Math.cos(pitch)];
    };
    for(const object of model.objects)for(const face of object.faces) {
      const points=face.indices.map(i=>rotate(object.vertices[i]));
      const normal=cross(sub(points[1],points[0]),sub(points[2],points[0])),length=Math.hypot(...normal);
      if(length<1e-8)throw new Error(`Degenerate face: ${object.name}`);
      const n=normal.map(v=>v/length),light=object.bone==='halo'?1:
        .78+.22*Math.max(0,n[0]*-.35+n[1]*.7+n[2]*.62);
      const hex=model.palette[face.material],color=[1,3,5].map(i=>Math.min(255,Math.round(parseInt(hex.slice(i,i+2),16)*light)));
      const screen=points.map(([x,y,z])=>[column*600+300+x*scale,row*750+385-y*scale,z]);
      for(let i=1;i<screen.length-1;i++)triangle([screen[0],screen[i],screen[i+1]],color,column,row);
    }
  }
}
fs.mkdirSync(path.dirname(output),{recursive:true});
writePng(output,width,height,rgba);
console.log(output);
