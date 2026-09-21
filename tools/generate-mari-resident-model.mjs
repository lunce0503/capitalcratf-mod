import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const outputDir = path.resolve(
  here,
  "../src/main/resources/assets/capitalcraft/models/entity/resident/mari"
);
const textureDir = path.resolve(
  here,
  "../src/main/resources/assets/capitalcraft/textures/entity/resident"
);

const vertices = [];
const objects = [];

function rotate([x, y, z], [rx = 0, ry = 0, rz = 0]) {
  const sx = Math.sin(rx), cx = Math.cos(rx);
  const sy = Math.sin(ry), cy = Math.cos(ry);
  const sz = Math.sin(rz), cz = Math.cos(rz);

  let py = y * cx - z * sx;
  let pz = y * sx + z * cx;
  y = py;
  z = pz;

  let px = x * cy + z * sy;
  pz = -x * sy + z * cy;
  x = px;
  z = pz;

  px = x * cz - y * sz;
  py = x * sz + y * cz;
  return [px, py, z];
}

function addMesh(name, material, localVertices, faces, center = [0, 0, 0], rotation = [0, 0, 0]) {
  const start = vertices.length + 1;
  for (const point of localVertices) {
    const transformed = rotate(point, rotation);
    vertices.push([
      transformed[0] + center[0],
      transformed[1] + center[1],
      transformed[2] + center[2]
    ]);
  }
  objects.push({ name, material, start, faces });
}

function addBox(name, center, size, material, rotation = [0, 0, 0]) {
  const [hx, hy, hz] = size.map(value => value / 2);
  addMesh(
    name,
    material,
    [
      [-hx, -hy, -hz], [hx, -hy, -hz], [hx, hy, -hz], [-hx, hy, -hz],
      [-hx, -hy, hz], [hx, -hy, hz], [hx, hy, hz], [-hx, hy, hz]
    ],
    [
      [1, 4, 3, 2], [5, 6, 7, 8], [1, 5, 8, 4],
      [2, 3, 7, 6], [4, 8, 7, 3], [1, 2, 6, 5]
    ],
    center,
    rotation
  );
}

function addFrustum(name, bottomY, topY, bottomWidth, topWidth, bottomDepth, topDepth, material, centerX = 0, centerZ = 0) {
  const bw = bottomWidth / 2, tw = topWidth / 2;
  const bd = bottomDepth / 2, td = topDepth / 2;
  addMesh(
    name,
    material,
    [
      [centerX - bw, bottomY, centerZ - bd], [centerX + bw, bottomY, centerZ - bd],
      [centerX + bw, bottomY, centerZ + bd], [centerX - bw, bottomY, centerZ + bd],
      [centerX - tw, topY, centerZ - td], [centerX + tw, topY, centerZ - td],
      [centerX + tw, topY, centerZ + td], [centerX - tw, topY, centerZ + td]
    ],
    [
      [1, 2, 3, 4], [5, 8, 7, 6], [1, 5, 6, 2],
      [2, 6, 7, 3], [3, 7, 8, 4], [4, 8, 5, 1]
    ]
  );
}

function addTriPrism(name, points, backZ, frontZ, material) {
  const local = [
    [points[0][0], points[0][1], backZ],
    [points[1][0], points[1][1], backZ],
    [points[2][0], points[2][1], backZ],
    [points[0][0], points[0][1], frontZ],
    [points[1][0], points[1][1], frontZ],
    [points[2][0], points[2][1], frontZ]
  ];
  addMesh(name, material, local, [
    [1, 3, 2], [4, 5, 6], [1, 2, 5, 4], [2, 3, 6, 5], [3, 1, 4, 6]
  ]);
}

function addRing(name, center, radius, thickness, material, segments = 16) {
  const segmentLength = 2 * radius * Math.sin(Math.PI / segments) * 1.08;
  for (let i = 0; i < segments; i++) {
    const angle = (i / segments) * Math.PI * 2;
    const x = center[0] + Math.cos(angle) * radius;
    const z = center[2] + Math.sin(angle) * radius;
    addBox(
      `${name}_${String(i).padStart(2, "0")}`,
      [x, center[1], z],
      [thickness, thickness, segmentLength],
      material,
      [0, -angle, 0]
    );
  }
}

function addHemTabs() {
  const frontZ = 0.345;
  for (let i = -4; i <= 4; i++) {
    const x = i * 0.09;
    addBox(`front_hem_gold_${i + 4}`, [x, 0.31, frontZ], [0.035, 0.15, 0.035], "gold");
  }
  for (const side of [-1, 1]) {
    for (let i = 0; i < 3; i++) {
      addBox(
        `${side < 0 ? "left" : "right"}_hem_gold_${i}`,
        [side * (0.43 - i * 0.015), 0.31, 0.19 - i * 0.15],
        [0.035, 0.15, 0.035],
        "gold"
      );
    }
  }
}

// Shoes, socks, and legs.
for (const side of [-1, 1]) {
  const label = side < 0 ? "left" : "right";
  const x = side * 0.17;
  addBox(`${label}_shoe`, [x, 0.09, 0.065], [0.26, 0.18, 0.40], "shoe_black");
  addBox(`${label}_shoe_strap`, [x, 0.17, 0.12], [0.20, 0.055, 0.32], "white");
  addBox(`${label}_sock`, [x, 0.29, -0.015], [0.18, 0.28, 0.19], "white");
  addBox(`${label}_ankle_band`, [x, 0.39, 0.01], [0.195, 0.055, 0.21], "gold");
}

// Layered nun dress and apron.
addFrustum("white_hem", 0.25, 0.45, 0.94, 0.82, 0.64, 0.54, "white");
addFrustum("black_skirt", 0.42, 1.05, 0.84, 0.56, 0.54, 0.34, "cloth_black");
addFrustum("white_apron", 0.29, 1.08, 0.49, 0.30, 0.035, 0.035, "white", 0, 0.295);
addHemTabs();

// Torso, collar, bow, and shoulder ornament.
addBox("torso", [0, 1.26, 0], [0.56, 0.48, 0.34], "cloth_black");
addTriPrism("white_collar", [[-0.23, 1.46], [0.23, 1.46], [0, 1.17]], 0.176, 0.205, "white");
addTriPrism("teal_bow_left", [[-0.02, 1.30], [-0.24, 1.37], [-0.15, 1.14]], 0.207, 0.238, "teal");
addTriPrism("teal_bow_right", [[0.02, 1.30], [0.24, 1.37], [0.15, 1.14]], 0.207, 0.238, "teal");
addBox("bow_brooch", [0, 1.31, 0.25], [0.105, 0.105, 0.055], "gold", [0, 0, Math.PI / 4]);
for (const side of [-1, 1]) {
  const label = side < 0 ? "left" : "right";
  addBox(`${label}_shoulder_gold`, [side * 0.315, 1.39, 0], [0.10, 0.21, 0.38], "gold");
  addBox(`${label}_upper_sleeve`, [side * 0.39, 1.22, 0.02], [0.25, 0.47, 0.32], "cloth_black", [0, 0, side * 0.11]);
  addBox(`${label}_lower_sleeve`, [side * 0.39, 0.91, 0.11], [0.23, 0.31, 0.28], "cloth_black", [-0.18, 0, -side * 0.08]);
  addBox(`${label}_cuff`, [side * 0.35, 0.76, 0.19], [0.235, 0.105, 0.285], "gold", [-0.18, 0, -side * 0.08]);
}
// Folded white gloves in front.
addBox("left_glove", [-0.10, 0.72, 0.31], [0.22, 0.16, 0.13], "white", [0.20, 0.08, -0.22]);
addBox("right_glove", [0.10, 0.72, 0.315], [0.22, 0.16, 0.13], "white", [0.20, -0.08, 0.22]);

// Head and hair.
addBox("head", [0, 1.81, 0.01], [0.70, 0.68, 0.61], "skin");
addBox("hair_back", [0, 1.81, -0.325], [0.76, 0.78, 0.18], "hair");
addBox("hair_top", [0, 2.14, -0.01], [0.73, 0.15, 0.64], "hair");
addBox("left_hair", [-0.34, 1.73, 0.02], [0.15, 0.72, 0.62], "hair");
addBox("right_hair", [0.34, 1.73, 0.02], [0.15, 0.72, 0.62], "hair");
addBox("bang_left", [-0.18, 2.04, 0.335], [0.24, 0.32, 0.08], "hair", [0, 0, -0.09]);
addBox("bang_center", [0, 2.02, 0.34], [0.19, 0.35, 0.08], "hair");
addBox("bang_right", [0.18, 2.04, 0.335], [0.24, 0.32, 0.08], "hair", [0, 0, 0.09]);

// Face details.
for (const side of [-1, 1]) {
  const label = side < 0 ? "left" : "right";
  addBox(`${label}_eye_white`, [side * 0.145, 1.88, 0.326], [0.18, 0.105, 0.025], "white");
  addBox(`${label}_eye`, [side * 0.145, 1.875, 0.342], [0.105, 0.095, 0.025], "cyan");
  addBox(`${label}_pupil`, [side * 0.145, 1.87, 0.358], [0.04, 0.078, 0.018], "eye_dark");
  addBox(`${label}_blush`, [side * 0.24, 1.73, 0.327], [0.09, 0.028, 0.022], "blush");
}
addBox("mouth", [0, 1.70, 0.344], [0.075, 0.018, 0.018], "mouth");

// Long side lock and blocky braid on Mari's left.
addBox("left_long_lock", [-0.31, 1.45, 0.20], [0.15, 0.48, 0.16], "hair", [0.06, 0, -0.05]);
for (let i = 0; i < 4; i++) {
  addBox(
    `braid_${i}`,
    [-0.37 + (i % 2) * 0.025, 1.48 - i * 0.16, 0.24],
    [0.18 - i * 0.012, 0.18, 0.16 - i * 0.008],
    "hair",
    [0, 0, (i % 2 === 0 ? 1 : -1) * 0.18]
  );
}
addBox("braid_gold_band", [-0.35, 0.93, 0.24], [0.14, 0.07, 0.14], "gold");

// Nun veil and unmistakably long fox-ear silhouette.
addBox("veil_back", [0, 1.78, -0.45], [0.84, 1.12, 0.12], "veil_black");
addBox("veil_left_side", [-0.43, 1.76, -0.06], [0.16, 1.04, 0.68], "veil_black", [0, 0, -0.025]);
addBox("veil_right_side", [0.43, 1.76, -0.06], [0.16, 1.04, 0.68], "veil_black", [0, 0, 0.025]);
addTriPrism("left_fox_ear", [[-0.42, 2.08], [-0.11, 2.12], [-0.52, 2.72]], -0.39, 0.25, "veil_black");
addTriPrism("right_fox_ear", [[0.11, 2.12], [0.42, 2.08], [0.52, 2.72]], -0.39, 0.25, "veil_black");
addTriPrism("left_fox_ear_inner", [[-0.40, 2.18], [-0.20, 2.19], [-0.48, 2.59]], 0.253, 0.275, "ear_inner");
addTriPrism("right_fox_ear_inner", [[0.20, 2.19], [0.40, 2.18], [0.48, 2.59]], 0.253, 0.275, "ear_inner");
addBox("gold_head_band", [0, 2.11, 0.31], [0.77, 0.15, 0.08], "gold");

// Blue-white flower ornament near the left temple.
addBox("flower_center", [-0.34, 2.12, 0.38], [0.075, 0.075, 0.05], "flower_blue");
for (let i = 0; i < 5; i++) {
  const angle = (i / 5) * Math.PI * 2;
  addBox(
    `flower_petal_${i}`,
    [-0.34 + Math.cos(angle) * 0.085, 2.12 + Math.sin(angle) * 0.085, 0.375],
    [0.075, 0.12, 0.045],
    "white",
    [0, 0, angle]
  );
}
addBox("flower_leaf", [-0.45, 2.05, 0.355], [0.15, 0.075, 0.04], "teal", [0, 0, -0.55]);

// Floating geometric halo.
addRing("halo_ring", [0, 2.91, 0], 0.38, 0.055, "halo", 16);
for (let i = 0; i < 4; i++) {
  const angle = (i / 4) * Math.PI * 2;
  addBox(
    `halo_diamond_${i}`,
    [Math.cos(angle) * 0.50, 2.91, Math.sin(angle) * 0.50],
    [0.17, 0.045, 0.17],
    "halo",
    [0, angle, Math.PI / 4]
  );
}
addBox("halo_center_cross_x", [0, 2.91, 0], [0.34, 0.035, 0.055], "halo");
addBox("halo_center_cross_z", [0, 2.91, 0], [0.055, 0.035, 0.34], "halo");

const obj = [
  "# CapitalCraft Mari resident fan model",
  "# Generated by tools/generate-mari-resident-model.mjs",
  "mtllib mari-resident.mtl",
  ""
];
for (const [x, y, z] of vertices) obj.push(`v ${x.toFixed(6)} ${y.toFixed(6)} ${z.toFixed(6)}`);
obj.push("");
for (const object of objects) {
  obj.push(`o ${object.name}`);
  obj.push(`usemtl ${object.material}`);
  for (const face of object.faces) {
    obj.push(`f ${face.map(index => object.start + index - 1).join(" ")}`);
  }
  obj.push("");
}

const materials = {
  cloth_black: { kd: "0.055 0.052 0.065", roughness: "0.92" },
  veil_black: { kd: "0.035 0.034 0.045", roughness: "0.96" },
  shoe_black: { kd: "0.040 0.038 0.046", roughness: "0.70" },
  white: { kd: "0.930 0.915 0.900", roughness: "0.88" },
  skin: { kd: "0.965 0.735 0.655", roughness: "0.90" },
  blush: { kd: "0.960 0.480 0.500", roughness: "0.90" },
  mouth: { kd: "0.520 0.175 0.190", roughness: "0.90" },
  hair: { kd: "0.965 0.475 0.350", roughness: "0.86" },
  cyan: { kd: "0.205 0.745 0.820", roughness: "0.35" },
  eye_dark: { kd: "0.035 0.145 0.180", roughness: "0.35" },
  teal: { kd: "0.185 0.455 0.490", roughness: "0.82" },
  gold: { kd: "0.675 0.520 0.210", roughness: "0.55" },
  halo: { kd: "0.980 0.765 0.235", roughness: "0.30", ke: "0.22 0.13 0.02" },
  ear_inner: { kd: "0.300 0.255 0.295", roughness: "0.92" },
  flower_blue: { kd: "0.420 0.700 0.790", roughness: "0.84" }
};

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function pngChunk(type, data) {
  const typeBuffer = Buffer.from(type, "ascii");
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const checksum = Buffer.alloc(4);
  checksum.writeUInt32BE(crc32(Buffer.concat([typeBuffer, data])));
  return Buffer.concat([length, typeBuffer, data, checksum]);
}

function createPaletteTexture() {
  const width = 256;
  const height = 256;
  const cell = 64;
  const pixels = Buffer.alloc((width * 4 + 1) * height);
  const entries = Object.entries(materials);

  for (let y = 0; y < height; y++) {
    const row = y * (width * 4 + 1);
    pixels[row] = 0;
    for (let x = 0; x < width; x++) {
      const cellIndex = Math.floor(y / cell) * 4 + Math.floor(x / cell);
      const raw = entries[Math.min(cellIndex, entries.length - 1)][1].kd
        .split(" ")
        .map(value => Math.round(Number(value) * 255));
      const edgeShade = ((x + y) % 16 === 0) ? 0.96 : 1;
      const offset = row + 1 + x * 4;
      pixels[offset] = Math.round(raw[0] * edgeShade);
      pixels[offset + 1] = Math.round(raw[1] * edgeShade);
      pixels[offset + 2] = Math.round(raw[2] * edgeShade);
      pixels[offset + 3] = 255;
    }
  }

  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 6;
  const png = Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    pngChunk("IHDR", header),
    pngChunk("IDAT", zlib.deflateSync(pixels, { level: 9 })),
    pngChunk("IEND", Buffer.alloc(0))
  ]);
  fs.mkdirSync(textureDir, { recursive: true });
  fs.writeFileSync(path.join(textureDir, "mari.png"), png);
}

const usedMaterials = new Set(objects.map(object => object.material));
for (const material of usedMaterials) {
  if (!materials[material]) throw new Error(`Missing material definition: ${material}`);
}
for (const object of objects) {
  for (const face of object.faces) {
    for (const index of face) {
      const absoluteIndex = object.start + index - 1;
      if (absoluteIndex < 1 || absoluteIndex > vertices.length) {
        throw new Error(`Invalid face index in ${object.name}: ${absoluteIndex}`);
      }
    }
  }
}

const bounds = vertices.reduce(
  (result, [x, y, z]) => ({
    min: [
      Math.min(result.min[0], x),
      Math.min(result.min[1], y),
      Math.min(result.min[2], z)
    ],
    max: [
      Math.max(result.max[0], x),
      Math.max(result.max[1], y),
      Math.max(result.max[2], z)
    ]
  }),
  { min: [Infinity, Infinity, Infinity], max: [-Infinity, -Infinity, -Infinity] }
);
const dimensions = bounds.max.map((value, index) => Number((value - bounds.min[index]).toFixed(4)));

const mtl = ["# Material palette for mari-resident.obj", ""];
for (const [name, material] of Object.entries(materials)) {
  mtl.push(`newmtl ${name}`);
  mtl.push(`Kd ${material.kd}`);
  mtl.push("Ka 0.020 0.020 0.020");
  mtl.push("Ks 0.080 0.080 0.080");
  mtl.push(`Pr ${material.roughness}`);
  if (material.ke) mtl.push(`Ke ${material.ke}`);
  mtl.push("d 1.0", "illum 2", "");
}

const manifest = {
  format: "wavefront-obj",
  character: "Mari-inspired CapitalCraft resident fan model",
  coordinateSystem: "Y-up, meters-like Minecraft block units",
  bounds: {
    min: bounds.min.map(value => Number(value.toFixed(4))),
    max: bounds.max.map(value => Number(value.toFixed(4)))
  },
  dimensions: { width: dimensions[0], height: dimensions[1], depth: dimensions[2] },
  objectCount: objects.length,
  vertexCount: vertices.length,
  materials: Object.keys(materials),
  foxEarParts: ["left_fox_ear", "right_fox_ear", "left_fox_ear_inner", "right_fox_ear_inner"],
  notes: [
    "Long pointed veil ears are intentionally modeled as fox ears.",
    "Import mari-resident.obj with mari-resident.mtl into Blockbench, Blender, or another OBJ editor.",
    "The model is an original low-poly fan interpretation based on user-supplied references."
  ]
};

fs.mkdirSync(outputDir, { recursive: true });
fs.writeFileSync(path.join(outputDir, "mari-resident.obj"), `${obj.join("\n")}\n`);
fs.writeFileSync(path.join(outputDir, "mari-resident.mtl"), `${mtl.join("\n")}\n`);
fs.writeFileSync(path.join(outputDir, "mari-resident-model.json"), `${JSON.stringify(manifest, null, 2)}\n`);
createPaletteTexture();

console.log(`Generated ${objects.length} objects, ${vertices.length} vertices, and mari.png`);
