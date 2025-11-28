from flask import Flask, request, jsonify
from datetime import datetime, timezone

app = Flask(__name__)
MAX_MINUTES = 10  # 최근 10분만 사용

# ---------- 헬퍼 함수들 ----------

def parse_iso_datetime(dt_str: str) -> datetime | None:
    """
    ISO 형식 문자열을 datetime 으로 파싱.
    (예: "2025-11-28T12:30:00")
    """
    try:
        # 타임존 정보가 없으면 naive datetime 으로 파싱
        return datetime.fromisoformat(dt_str.replace("Z", "+00:00"))
    except Exception:
        return None


def compute_wait_time(votes: list[dict], base_time: datetime) -> int | None:
    """
    votes 리스트와 기준 시간(base_time)을 받아
    '최근 10분 이내' 투표만 사용해서 예측 대기시간(분)을 계산한다.

    - votes: [{ "votedTime": "...", "weight": 3, "waitingTime": 15 }, ...]
    - 반환: int(분) 또는 None(계산 불가)
    """

    effective_votes = []

    for v in votes:
        try:
            voted_time_str = v.get("votedTime")
            weight = int(v.get("weight"))
            waiting_time = float(v.get("waitingTime"))
        except (TypeError, ValueError):
            # 잘못된 형식의 vote는 무시
            continue

        voted_time = parse_iso_datetime(voted_time_str)
        if voted_time is None:
            continue

        # base_time 기준으로 얼마나 지난 투표인지 (분 단위)
        delta_minutes = (base_time - voted_time).total_seconds() / 60.0

        # 미래 시간(음수) → 무시
        if delta_minutes < 0:
            continue

        # 10분보다 오래된 투표 → 무시
        if delta_minutes > MAX_MINUTES:
            continue

        # (원하면 여기서 추가 시간 가중치 줄 수도 있음)
        # 예: 최근일수록 더 크게 반영하고 싶다면
        # time_weight = 1.0 / (1.0 + delta_minutes / 5.0)  # 0~10분에 대해 완만한 감소
        time_weight = 1.0 / (1.0 + delta_minutes / 5.0) # 일단 시간 가중치는 1로 두고, weight만 사용

        final_weight = weight * time_weight

        if final_weight <= 0:
            continue

        effective_votes.append((waiting_time, final_weight))

    # 유효한 투표가 하나도 없으면 None
    if not effective_votes:
        return None

    # 가중 평균 계산
    numerator = sum(wt * w for (wt, w) in effective_votes)
    denominator = sum(w for (_, w) in effective_votes)

    if denominator == 0:
        return None

    avg_wait = numerator / denominator

    if avg_wait < 0:
        avg_wait = 0

    return int(round(avg_wait))

    # 유효한 투표만 모을 리스트
    effective_votes = []

    for v in votes:
        try:
            voted_time_str = v.get("votedTime")
            weight = int(v.get("weight"))
            waiting_time = float(v.get("waitingTime"))
        except (TypeError, ValueError):
            # 잘못된 형식의 vote는 무시
            continue

        voted_time = parse_iso_datetime(voted_time_str)
        if voted_time is None:
            continue

        # (선택) 시간 가중치 적용을 위한 delta 계산 (분 단위)
        delta_minutes = abs((base_time - voted_time).total_seconds()) / 60.0

        # 여기서는 "최근일수록 더 큰 가중치" 같은 로직을 넣을 수 있음
        # 예시: 0분 차이면 1.0, 30분 차이면 0.5, 60분 차이면 0.25 ...
        time_weight = 1.0 / (1.0 + delta_minutes / 30.0)

        # 최종 가중치 = 혼잡도 weight * 시간 가중치
        final_weight = weight * time_weight

        if final_weight <= 0:
            continue

        effective_votes.append((waiting_time, final_weight))

    if not effective_votes:
        # 유효한 투표가 하나도 없으면 None
        return None

    # 가중 평균 계산
    numerator = sum(wt * w for (wt, w) in effective_votes)
    denominator = sum(w for (_, w) in effective_votes)

    if denominator == 0:
        return None

    avg_wait = numerator / denominator

    # 음수 방지, 적당히 반올림
    if avg_wait < 0:
        avg_wait = 0

    return int(round(avg_wait))


# ---------- Flask 엔드포인트 ----------

@app.route("/api/wait-time", methods=["POST"])
def get_wait_time():
    """
    스프링에서 보내는 JSON:
    {
      "restaurantId": 1,
      "baseTime": "2025-11-28T12:30:00",
      "windowMinutes": 30,
      "votes": [
        { "votedTime": "...", "weight": 3, "waitingTime": 15 },
        ...
      ]
    }
    """
    data = request.get_json()

    if not data:
        return jsonify({"error": "Request body is empty"}), 400

    # 필수 필드 체크
    if "restaurantId" not in data or "baseTime" not in data or "votes" not in data:
        return jsonify({"error": "Missing required fields"}), 400

    restaurant_id = data.get("restaurantId")
    base_time_str = data.get("baseTime")
    votes = data.get("votes") or []

    base_time = parse_iso_datetime(base_time_str)
    if base_time is None:
        return jsonify({"error": "Invalid baseTime format"}), 400

    # 예측 대기시간 계산
    wait_time_min = compute_wait_time(votes, base_time)

    if wait_time_min is None:
        status = "NO_DATA"
    else:
        status = "SUCCESS"

    response = {
        "restaurantId": restaurant_id,
        "baseTime": base_time_str,
        "waitTimeMin": wait_time_min,
        "status": status,
        "voteCount": len(votes)  # 디버깅/표시용
    }

    return jsonify(response), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
