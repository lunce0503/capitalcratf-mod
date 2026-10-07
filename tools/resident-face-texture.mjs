// Flat cuboid heads with painted facial features; no eye/nose/mouth geometry.
import fs from 'node:fs';
import path from 'node:path';
import {writePng} from './mari-png.mjs';

export const atlasSize=256;
export const faceTile={x:96,y:16,size:128};
export const referenceHeadProportions={
  scale:[1.18,1.06,1.10],anchorY:1.61,shape:'near-cube',dimensions:[.7198,.6572,.638],
  eyeStyle:'wide-angular-block',eyeWidthFraction:.3125,lashSafeMarginPixels:12
};
export function residentHeadProportions(name) {
  if(name!=='mari')return referenceHeadProportions;
  return {...referenceHeadProportions,shape:'attached-blender-mirrored-cuboid',dimensions:[.54972,.43554,.5357],
    eyeStyle:'attached-mirrored-block-eye',eyeWidthFraction:.21875,lashSafeMarginPixels:5};
}
const rgb=hex=>[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16));

// Apply one affine transform to the entire head assembly, not just the face.
// Shared ear/crown seams and head/hair pivots stay aligned during animation.
export function applyReferenceHeadProportions(objects,bones) {
  const groups=new Set(['head','hair','veil','halo']);
  const {scale:[sx,sy,sz],anchorY}=referenceHeadProportions;
  const transform=([x,y,z])=>[x*sx,anchorY+(y-anchorY)*sy,z*sz].map(v=>+v.toFixed(5));
  for(const object of objects)if(groups.has(object.bone))object.vertices=object.vertices.map(transform);
  for(const bone of groups)if(bones[bone])bones[bone]=transform(bones[bone]);
}

export function cuboidFace(mesh,objects,name) {
  // Mari uses the exact bounds edited in the supplied Blender file. Seia keeps
  // the existing near-cube head; importing Mari must not mutate Seia's model.
  const mari=name==='mari';
  const w=mari?.32:.305,lo=mari?1.61:1.56,hi=mari?2.15:2.18;
  const back=mari?-.205:-.25,front=mari?.282:.33;
  mesh('flat_face','head',[[-w,lo,back],[w,lo,back],[w,hi,back],[-w,hi,back],
    [-w,lo,front],[w,lo,front],[w,hi,front],[-w,hi,front]],
    [[0,3,2,1],[4,5,6,7],[0,4,7,3],[1,2,6,5],[3,7,6,2],[0,1,5,4]],'skin');
  const face=objects.at(-1);
  const frontFace=face.faces.find(f=>f.indices.every(i=>face.vertices[i][2]===front));
  frontFace.uv=frontFace.indices.map(i=>{
    const [x,y]=face.vertices[i];
    return [(faceTile.x+.5+(x+w)/(2*w)*(faceTile.size-1))/atlasSize,
      (faceTile.y+.5+(hi-y)/(hi-lo)*(faceTile.size-1))/atlasSize];
  });
}

export function paintFace(name,palette) {
  const size=128,aa=4,canvas=size*aa,pixels=Buffer.alloc(canvas*canvas*4);
  const fill=rgb(palette.skin);
  for(let i=0;i<canvas*canvas;i++)pixels.set([...fill,255],i*4);
  const put=(x,y,color)=>pixels.set([...color,255],(y*canvas+x)*4);
  function shape(test,color) {
    const c=typeof color==='string'?rgb(color):color;
    for(let y=0;y<canvas;y++)for(let x=0;x<canvas;x++) {
      const px=(x+.5)/aa,py=(y+.5)/aa;
      if(test(px,py))put(x,y,typeof c==='function'?c(px,py):c);
    }
  }
  function polygon(points,color) {
    shape((x,y)=>{
      let inside=false;
      for(let i=0,j=points.length-1;i<points.length;j=i++) {
        const [a,b]=points[i],[c,d]=points[j];
        if((b>y)!==(d>y) && x<(c-a)*(y-b)/(d-b)+a)inside=!inside;
      }
      return inside;
    },color);
  }
  function stroke(points,color,r=.7) {
    shape((x,y)=>points.slice(1).some(([bx,by],i)=>{
      const [ax,ay]=points[i],dx=bx-ax,dy=by-ay;
      const t=Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy)));
      return (x-ax-t*dx)**2+(y-ay-t*dy)**2<=r*r;
    }),color);
  }
  const calm=name==='seia',ink=calm?'#514049':'#39333e';
  for(const side of [-1,1]) {
    const cx=64+side*28;
    const upper=calm?72:70,lower=calm?88:89;
    // Each 40px eye spans 31.25% of the 128px face, approximately one third.
    // The straight opening, narrow vertical iris and heavy angular lid match the reference.
    polygon([[cx-20,upper+3],[cx+20,upper+3],[cx+18,lower],
      [cx-18,lower]],'#f1e8dd');
    shape((x,y)=>x>=cx-5 && x<cx+5 && y>=upper+3 && y<lower,
      calm?'#98677f':'#527d88');
    shape((x,y)=>x>=cx-5 && x<cx+5 && y>=upper+10 && y<lower,
      calm?'#bc859e':'#75a5af');
    shape((x,y)=>x>=cx-2.3 && x<cx+2.3 && y>=upper+4 && y<lower-1,
      calm?'#534051':'#29404d');
    // Positive local X points outward. The 24px wing leaves a measured 12px
    // margin to the face-tile edge, so antialiasing cannot crop the eyelashes.
    polygon([[-19,1],[-10,-1],[0,0],[10,-2],[23,-6],[21,3],[10,5],
      [0,4],[-10,4],[-19,3]].map(([dx,dy])=>[cx+side*dx,upper+dy]),ink);
    polygon([[18,-3],[24,-9],[22,2]].map(([dx,dy])=>[cx+side*dx,upper+dy]),ink);
    stroke([[cx-15,61],[cx,59.5],[cx+14,60.5]],palette.hairShade,.8);
  }
  // Eyes and brows only. Leave the nose/mouth area uniformly skin-coloured.
  const out=Buffer.alloc(size*size*4);
  for(let y=0;y<size;y++)for(let x=0;x<size;x++) {
    const sum=[0,0,0];
    for(let dy=0;dy<aa;dy++)for(let dx=0;dx<aa;dx++) {
      const p=((y*aa+dy)*canvas+x*aa+dx)*4;
      for(let c=0;c<3;c++)sum[c]+=pixels[p+c];
    }
    out.set([...sum.map(v=>Math.round(v/(aa*aa))),255],(y*size+x)*4);
  }
  return out;
}

export function faceUvs(face,palette) {
  if(face.uv)return face.uv;
  const m=Object.keys(palette).indexOf(face.material),x=(m%8)*8,y=Math.floor(m/8)*8;
  return face.indices.map((_,i)=>[(x+(i===0||i===3?3:5))/atlasSize,(y+(i<2?3:5))/atlasSize]);
}

export function writeResidentAssets(base,output,name,bones,palette,objects,extra={}) {
  fs.mkdirSync(output,{recursive:true});
  const data={format:2,texture:`capitalcraft:textures/entity/resident/${name}.png`,
    textureSize:[atlasSize,atlasSize],unitsPerBlock:16,modelScale:12,bones,palette,objects};
  fs.writeFileSync(path.join(output,`${name}-mesh.json`),JSON.stringify(data)+'\n');
  const obj=['# Runtime geometry; Y up, +Z front; OBJ units are blocks.',`mtllib ${name}-resident.mtl`];
  let vertex=1,uvIndex=1;
  for(const o of objects) {
    obj.push(`o ${o.name}`);
    for(const p of o.vertices)obj.push(`v ${p.map(v=>(v*.75).toFixed(6)).join(' ')}`);
    for(const f of o.faces) {
      for(const [u,v] of faceUvs(f,palette))obj.push(`vt ${u.toFixed(8)} ${(1-v).toFixed(8)}`);
      obj.push(`usemtl ${f.uv?'face_texture':f.material}`);
      obj.push(`f ${f.indices.map((i,j)=>`${vertex+i}/${uvIndex+j}`).join(' ')}`);
      uvIndex+=f.indices.length;
    }
    vertex+=o.vertices.length;
  }
  fs.writeFileSync(path.join(output,`${name}-resident.obj`),obj.join('\n')+'\n');
  const materials=Object.entries(palette).map(([k,c])=>
    `newmtl ${k}\nKd ${rgb(c).map(v=>(v/255).toFixed(6)).join(' ')}\nd 1\nillum 1`);
  materials.push(`newmtl face_texture\nKd 1 1 1\nd 1\nillum 1\nmap_Kd ${name}-atlas.png`);
  fs.writeFileSync(path.join(output,`${name}-resident.mtl`),materials.join('\n\n')+'\n');
  const pixels=Buffer.alloc(atlasSize*atlasSize*4),skin=rgb(palette.skin),colors=Object.values(palette).map(rgb);
  for(let y=0;y<atlasSize;y++)for(let x=0;x<atlasSize;x++) {
    const color=x<64&&y<64?(colors[Math.floor(y/8)*8+Math.floor(x/8)]??colors[0]):skin;
    pixels.set([...color,255],(y*atlasSize+x)*4);
  }
  const face=paintFace(name,palette);
  for(let y=0;y<128;y++)face.copy(pixels,((y+faceTile.y)*atlasSize+faceTile.x)*4,y*128*4,(y+1)*128*4);
  writePng(path.join(base,`src/main/resources/assets/capitalcraft/textures/entity/resident/${name}.png`),atlasSize,atlasSize,pixels);
  writePng(path.join(output,`${name}-atlas.png`),atlasSize,atlasSize,pixels);
  writePng(path.join(output,`${name}-face.png`),128,128,face);
  const ys=objects.flatMap(o=>o.vertices.map(p=>p[1]));
  const summary={format:'shared-mesh-v2',objectCount:objects.length,vertexCount:vertex-1,
    faceCount:objects.reduce((n,o)=>n+o.faces.length,0),heightBlocks:+((Math.max(...ys)-Math.min(...ys))*.75).toFixed(4),
    faceStyle:'textured-cuboid',facialFeatures:'eyes-only',textureSize:[atlasSize,atlasSize],headProportions:residentHeadProportions(name),
    foxEarParts:['left_fox_ear','right_fox_ear','left_fox_ear_inner','right_fox_ear_inner'],
    ...extra,runtimeMesh:`${name}-mesh.json`,preview:`${name}-preview.png`};
  fs.writeFileSync(path.join(output,`${name}-resident-model.json`),JSON.stringify(summary,null,2)+'\n');
  console.log(summary);
}
