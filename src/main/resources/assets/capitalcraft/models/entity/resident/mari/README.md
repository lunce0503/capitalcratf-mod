# Mari resident model

CapitalCraft의 마리 주민용 저폴리 팬 모델이다. 사용자 제공 참고 이미지의 특징을
Minecraft에서 읽히는 면 분할 메시로 재구성했다. 게임, OBJ, 프리뷰가 모두
`mari-mesh.json`에서 생성되므로 서로 같은 형상을 사용한다.

## 파일

- `mari-mesh.json`: Fabric 런타임에서 직접 읽는 본·면·팔레트 메시
- `mari-resident.obj`: 같은 메시의 Blockbench/Blender 편집용 내보내기
- `mari-resident.mtl`: 색상 및 재질 팔레트
- `mari-resident-model.json`: 개수, 크기, 주요 파츠 메타데이터
- `mari-preview.png`: 런타임 메시를 소프트웨어 렌더링한 4방향 프리뷰
- `mari-turnaround.png`: 모델링용 턴어라운드 원화
- `../../../../textures/entity/resident/mari.png`: 런타임 모델 팔레트 텍스처

## Blockbench에서 열기

1. Blockbench에서 `File > Import > Wavefront OBJ`를 선택한다.
2. `mari-resident.obj`를 연다.
3. 같은 폴더의 `mari-resident.mtl`을 함께 유지한다.
4. 게임용 원본은 `mari-mesh.json`이므로 OBJ를 수정했다면 생성 스크립트에도
   같은 변경을 반영한다.

## 크기

- Y축이 위쪽이다.
- 생성 좌표에 `0.75`를 곱한 값이 OBJ 단위이다.
- 게임에서는 생성 좌표당 12 모델 픽셀을 사용한다.
- 헤일로를 포함한 전체 높이는 약 `2.15`블록이다.

`left_fox_ear`, `right_fox_ear`와 내부 파츠는 길고 바깥쪽으로 벌어진 여우귀 실루엣으로 제작했다.

실제 Fabric 렌더링은 `MariMeshLoader`가 `mari-mesh.json`을 Minecraft
`ModelPart`로 변환하고 `textures/entity/resident/mari.png` 팔레트를 적용한다.
머리, 베일, 양팔, 양다리, 헤일로는 독립 본으로 움직인다.

## 재생성 및 검증

```bash
node tools/generate-mari-resident-model.mjs
node tools/render-mari-preview.mjs
JAVA_HOME=/home/kwon/.local/share/jdks/jdk-21.0.11+10 \
  PATH=/home/kwon/.local/share/jdks/jdk-21.0.11+10/bin:$PATH \
  ./gradlew clean check --console=plain
```
