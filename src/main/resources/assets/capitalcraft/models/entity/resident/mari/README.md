# Mari resident model

CapitalCraft의 마리 주민용 저폴리 팬 모델이다. 사용자 제공 참고 이미지의 특징을
Minecraft에서 읽히는 면 분할 메시로 재구성했다. 게임, OBJ, 프리뷰가 모두
`mari-mesh.json`에서 생성되므로 서로 같은 형상을 사용한다.

## 파일

- `mari-mesh.json`: Fabric 런타임에서 직접 읽는 본·면·팔레트 메시
- `mari-resident.obj`: 같은 메시의 Blockbench/Blender 편집용 내보내기
- `mari-resident.mtl`: 색상 및 재질 팔레트
- `mari-atlas.png`: OBJ 얼굴 재질에 사용하는 256×256 텍스처
- `mari-face.png`: 눈·눈썹을 그린 128×128 얼굴 텍스처 (코·입 생략)
- `mari-resident-model.json`: 개수, 크기, 주요 파츠 메타데이터
- `mari-preview.png`: 런타임 메시를 소프트웨어 렌더링한 4방향 프리뷰
- `mari-turnaround.png`: 모델링용 턴어라운드 원화
- `../../../../textures/entity/resident/mari.png`: 런타임 모델 팔레트 텍스처

## Blockbench에서 열기

1. Blockbench에서 `File > Import > Wavefront OBJ`를 선택한다.
2. `mari-resident.obj`를 연다.
3. 같은 폴더의 `mari-resident.mtl`, `mari-atlas.png`를 함께 유지한다.
4. 게임용 원본은 `mari-mesh.json`이므로 OBJ를 수정했다면 생성 스크립트에도
   같은 변경을 반영한다.

## 크기

- Y축이 위쪽이다.
- 생성 좌표에 `0.75`를 곱한 값이 OBJ 단위이다.
- 게임에서는 생성 좌표당 12 모델 픽셀을 사용한다.
- 헤일로를 포함한 전체 높이는 약 `2.21`블록이다.

`left_fox_ear`, `right_fox_ear`와 내부 파츠는 길고 바깥쪽으로 벌어진 여우귀 실루엣으로 제작했다.

실제 Fabric 렌더링은 `MariMeshLoader`가 `mari-mesh.json`을 Minecraft
`ModelPart`로 변환하고 `textures/entity/resident/mari.png` 팔레트를 적용한다.
머리, 베일, 양팔, 양다리, 헤일로는 독립 본으로 움직인다.

얼굴은 첨부 Blender 파일의 미러 모디파이어를 적용한 `flat_face` 하나로,
12정점·10면의 직육면체다. 앞면 두 면이 같은 눈 그림을 좌우로 미러 매핑한다.
볼, 코, 입, 눈의 돌출 메시나 베벨은 없다. 나머지 면은 피부색 팔레트를 사용한다.
코·입은 텍스처에서도 생략하며 눈 아래는 단색 피부다.
공유 생성기는 `tools/resident-face-texture.mjs`다.
참고 사진의 비율에 맞춰 머리·머리카락·귀·베일·헤일로에 동일한 변환을
적용한다(폭 1.18배, 높이 1.06배, 깊이 1.10배; 기준 Y 1.61).
첨부 파일의 내장 텍스처를 그대로 사용해 큰 청록색 눈동자와 두꺼운 속눈썹,
작은 흰 하이라이트를 표현한다.

최신 사용자 첨부 파일에서 `tools/import-mari-blend.py`로 평가된 메시와 UV,
내장 텍스처를 추출한다. 입력은 `tools/mari-blender-geometry-overrides.json`,
`tools/mari-blender-atlas.png`, `tools/mari-blender-face.png`에 저장한다.
JSON에는 원본 Blender 파일의 SHA256도 기록한다. 얼굴·상의·목·여우귀 안쪽
7개 오브젝트를 재생성 때 적용하며, 세이아는 변경하지 않는다.

```bash
blender -b mari-seia-textured-faces.blend --python tools/import-mari-blend.py
```

## 재생성 및 검증

```bash
node tools/generate-mari-resident-model.mjs
node tools/render-mari-preview.mjs
JAVA_HOME=/home/kwon/.local/share/jdks/jdk-25.0.4.1+1 \
  PATH=/home/kwon/.local/share/jdks/jdk-25.0.4.1+1/bin:$PATH \
  ./gradlew clean check --console=plain
```
