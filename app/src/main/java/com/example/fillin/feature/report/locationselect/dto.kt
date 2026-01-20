package com.example.fillin.feature.report.locationselect

// 카카오 역지오코딩 응답을 담는 데이터 클래스들
data class KakaoAddressResponse(
    val documents: List<AddressDocument>
) // 카카오 API가 던져주는 가장 큰 택배 박스입니다. 안에 여러 개의 AddressDocument(검색 결과들)를 리스트 형식으로 담고 있습니다.

data class AddressDocument(
    val address: AddressInfo?,      // 지번 주소 정보
    val road_address: RoadAddressInfo? // 도로명 주소 정보
) // 이 안에는 두 가지 종류의 주소 정보가 들어있습니다

data class AddressInfo(
    val address_name: String // 전체 지번 주소
)

data class RoadAddressInfo(
    val address_name: String // 전체 도로명 주소
) // address_name(전체 도로명 주소 이름) 같은 실제 글자 데이터가 들어있습니다.
