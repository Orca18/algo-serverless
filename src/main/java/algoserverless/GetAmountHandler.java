package algoserverless;

import algoserverless.service.AlgoService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

// 람다 함수가 호출될 때 호출됩니다. Let's take a look at the code snippet:
public class GetAmountHandler implements RequestHandler<String, Long> {
    // <String, Long> 은 각각 Request와 Response 유형
    // 문자열을 입력으로 받아들이고, Long 을 반환한다.
    // implements RequestHandler : 인터페이스를 사용하려면 @Override HanldeRequest 메서드를 구현해야합니다.
    AlgoService algoService = new AlgoService(
            "https://node.testnet.algoexplorerapi.io/",
            443,
            null,
            "https://algoindexer.testnet.algoexplorerapi.io/idx2",
            443);
    // 위의 AlgoService 객체를 초기화하기 위해 환경 변수에 포함된 정보를 사용했습니다.
    // 람다 함수에서는 System.getEnv() 호출을 사용하여 환경 변수를 검색할 수 있다.
    // 이런 방식으로, 우리는 민감한 정보를 코드에 노출시키지 않고 더 많은 재사용 가능성을 달성할 수 있다.


    // =====================================================================================
    // HandleRequest 메서드는 실수로 문자열을 받아들이고 Long 을 반환합니다.
    @Override
    public Long handleRequest(final String address, final Context context) {
        // Context context 에는 함수의 현재 인스턴스에 대한 유용한 정보가 포함되어 있습니다.
        // 람다가 호출 → Algorand 주소 전달 → 체크 (문자열)
        // 작업이 끝나면, 소유한 알고의 총량(long)을 리턴한다.

        // 람다 실행을 제어할 수 없기 때문에, 로그를 생성하는 것이 중요하다.
        // 이를 위해 다음과 같이 Context 객체가 제공하는 LambdaLogger 를 사용할 수 있다.
//        LambdaLogger logger = context.getLogger();
        // 위에서 AlgoService 개체를 인스턴스화 했고,
        // 아래에서 getAccountAmount 메서드를 호출하여, 전달한 주소가 소유한 Algos 의 금액을 검사
        Long amount = algoService.getAccountAmount(address).orElse(-1L);

        // getAccountAmount() 메서드는 Optional을 반환하므로 응답을 얻으려면 옵션이 비어있는지
        // (위 경우 오류를 나타내는 -1L을 반환함)
        // 반환되는 값이 포함되어 있는지 확인해야 됨.
//        logger.log("Amount: " + amount);
        return amount;
    }
}
