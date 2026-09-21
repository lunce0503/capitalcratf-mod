# Mari resident model

CapitalCraft 주민 외형 실험용 저폴리 팬 모델이다. 사용자 제공 참고 이미지의 특징을 Minecraft에서 읽히는 큐브/프리즘 실루엣으로 재구성했다.

## 파일

- `mari-resident.obj`: 실제 3D 메시
- `mari-resident.mtl`: 색상 및 재질 팔레트
- `mari-resident-model.json`: 크기, 파츠, 재질 메타데이터
- `mari-turnaround.png`: 모델링용 턴어라운드 원화
- `../../../../textures/entity/resident/mari.png`: 런타임 모델 팔레트 텍스처

## Blockbench에서 열기

1. Blockbench에서 `File > Import > Wavefront OBJ`를 선택한다.
2. `mari-resident.obj`를 연다.
3. 같은 폴더의 `mari-resident.mtl`을 함께 유지한다.
4. Fabric 엔티티 모델로 변환할 때는 머리, 몸, 팔, 치마, 베일, 여우귀, 머리카락, 헤일로를 별도 본으로 정리한다.

## 크기

- Y축이 위쪽이다.
- 전체 경계 크기는 약 `1.152 × 2.986 × 1.170` 단위이다.
- 여우귀를 포함한 몸체 상단은 `2.72`, 헤일로까지 포함한 전체 높이는 `2.986`이다.
- 실제 주민 렌더링 시 원하는 크기에 맞춰 전체 스케일을 조정한다.

`left_fox_ear`, `right_fox_ear`와 내부 파츠는 길고 바깥쪽으로 벌어진 여우귀 실루엣으로 제작했다.

실제 Fabric 렌더링은 `MariResidentModel`의 Minecraft 모델 파츠와
`textures/entity/resident/mari.png`를 사용한다. OBJ는 후속 Blockbench 편집과
고급 모델링을 위한 원본 자산이다.
