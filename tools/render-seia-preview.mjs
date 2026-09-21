// Four-view orthographic rasterizer of the exact runtime Seia mesh.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {writePng} from './mari-png.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../src/main/resources/assets/capitalcraft/models/entity/resident/seia');
const model=JSON.parse(fs.readFileSync(path.join(root,'seia-mesh.json'),'utf8'));
const width=1600,height=1000,rgba=Buffer.alloc(width*height*4),depth=new Float64Array(width*height).fill(-Infinity);
const angles=[0,Math.PI/6,-Math.PI/2,Math.PI],scale=300,pitch=.075;
for(let y=0;y<height;y++)for(let x=0;x<width;x++){const g=Math.round(235-y/height*13),p=(y*width+x)*4;rgba.set([g+3,g+2,g,255],p);}
const sub=(a,b)=>a.map((v,i)=>v-b[i]);const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
function triangle(points,color){const [[ax,ay,az],[bx,by,bz],[cx,cy,cz]]=points,den=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy);if(Math.abs(den)<1e-6)return;const minX=Math.max(0,Math.floor(Math.min(ax,bx,cx))),maxX=Math.min(width-1,Math.ceil(Math.max(ax,bx,cx))),minY=Math.max(0,Math.floor(Math.min(ay,by,cy))),maxY=Math.min(height-1,Math.ceil(Math.max(ay,by,cy)));for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){const a=((by-cy)*(x+.5-cx)+(cx-bx)*(y+.5-cy))/den,b=((cy-ay)*(x+.5-cx)+(ax-cx)*(y+.5-cy))/den,c=1-a-b;if(a<0||b<0||c<0)continue;const z=a*az+b*bz+c*cz,p=y*width+x;if(z<depth[p])continue;depth[p]=z;rgba.set([...color,255],p*4);}}
for(let view=0;view<angles.length;view++){const a=angles[view],ox=view*400+200,oy=932,rotate=([x,y,z])=>{const xx=x*Math.cos(a)+z*Math.sin(a),zz=-x*Math.sin(a)+z*Math.cos(a);return[xx,y*Math.cos(pitch)-zz*Math.sin(pitch),y*Math.sin(pitch)+zz*Math.cos(pitch)];};for(let y=oy-11;y<oy+17;y++)for(let x=ox-112;x<ox+112;x++){const d=((x-ox)/112)**2+((y-oy-2)/14)**2;if(d<1){const p=(y*width+x)*4,k=1-.12*(1-d);for(let c=0;c<3;c++)rgba[p+c]=Math.round(rgba[p+c]*k);}}for(const obj of model.objects)for(const face of obj.faces){const points=face.indices.map(i=>rotate(obj.vertices[i]));let n=cross(sub(points[1],points[0]),sub(points[2],points[0])),len=Math.hypot(...n);if(len<1e-8)throw Error(`Degenerate face: ${obj.name}`);n=n.map(v=>v/len);const lighting=obj.bone==='halo'?1:Math.min(1.13,.77+.23*Math.max(0,n[0]*-.35+n[1]*.7+n[2]*.62)),hex=model.palette[face.material],color=[1,3,5].map(i=>Math.min(255,Math.round(parseInt(hex.slice(i,i+2),16)*lighting))),screen=points.map(([x,y,z])=>[ox+x*scale,oy-y*scale,z]);for(let i=1;i<screen.length-1;i++)triangle([screen[0],screen[i],screen[i+1]],color);}}
const file=path.join(root,'seia-preview.png');writePng(file,width,height,rgba);console.log(file);
