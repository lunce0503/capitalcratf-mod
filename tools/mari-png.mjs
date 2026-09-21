import fs from 'node:fs';
import zlib from 'node:zlib';

function chunk(type,data) {
  const kind=Buffer.from(type),body=Buffer.concat([kind,data]);
  let crc=0xffffffff;
  for(const byte of body) {crc^=byte; for(let i=0;i<8;i++)crc=(crc>>>1)^(0xedb88320&-(crc&1));}
  const size=Buffer.alloc(4),sum=Buffer.alloc(4);
  size.writeUInt32BE(data.length);sum.writeUInt32BE((crc^0xffffffff)>>>0);
  return Buffer.concat([size,body,sum]);
}
export function writePng(file,width,height,rgba) {
  const rows=Buffer.alloc((width*4+1)*height);
  for(let y=0;y<height;y++)rgba.copy(rows,y*(width*4+1)+1,y*width*4,(y+1)*width*4);
  const header=Buffer.alloc(13);
  header.writeUInt32BE(width);header.writeUInt32BE(height,4);header[8]=8;header[9]=6;
  fs.writeFileSync(file,Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(rows)),chunk('IEND',Buffer.alloc(0))]));
}
