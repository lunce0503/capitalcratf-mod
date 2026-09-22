// Closed cheek/jaw surface shared by the runtime mesh and OBJ generators.
// Extra bevels round the cheeks while retaining a broad area for the eyes.
export function roundedFace(mesh, sections) {
  const outline = [[-.72,1],[-.4,1],[0,1],[.4,1],[.72,1],[.9,.87],[1,.6],[1,-.4],[.8,-.85],
    [.42,-1],[-.42,-1],[-.8,-.85],[-1,-.4],[-1,.6],[-.9,.87]];
  const sides = outline.length;
  const vertices = sections.flatMap(([y,w,d,z,puff=0]) => outline.map(([x,t]) =>
    [x*w,y,z+t*d-(t===1?puff*(1-(x/.72)**2):0)]));
  const faces = [];
  for (let j=0;j<sections.length-1;j++) for (let i=0;i<sides;i++) {
    const a=j*sides+i,b=j*sides+(i+1)%sides,c=b+sides,d=a+sides;
    // Ring widths and depths vary independently; triangles avoid warped quads.
    faces.push([a,b,c],[a,c,d]);
  }
  for (const j of [0,sections.length-1]) {
    const center=vertices.length;
    vertices.push([0,sections[j][0],sections[j][3]]);
    for(let i=0;i<sides;i++) faces.push([center,j*sides+i,j*sides+(i+1)%sides]);
  }
  mesh('soft_face','head',vertices,faces,'skin');

  // Put blush/mouth directly on the actual surface, including its side bevels.
  return (x,y,relief=.006) => {
    let front=-Infinity;
    for (const indices of faces) {
      const [a,b,c]=indices.map(i=>vertices[i]);
      const den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);
      if (Math.abs(den)<1e-10) continue;
      const u=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den;
      const v=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den,w=1-u-v;
      if (Math.min(u,v,w)>=-1e-7) front=Math.max(front,u*a[2]+v*b[2]+w*c[2]);
    }
    if (!Number.isFinite(front)) throw new Error(`Facial detail outside face: ${x}, ${y}`);
    return [x,y,front+relief];
  };
}
