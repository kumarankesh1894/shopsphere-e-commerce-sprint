# Payment Service Testing Guide (Backend Only)

This guide explains how to test the payment flow without frontend.

## 1) Prerequisites

Make sure these services are running:

- Eureka Server (`8761`)
- API Gateway (`8080`)
- Order Service (`8083`)
- Payment Service (`8084`)

## 2) Login and Get JWT

Use your auth API and copy the JWT token.

Use this header for gateway-protected endpoints:

```http
Authorization: Bearer <JWT_TOKEN>
```

## 3) Ensure Order Is in `CHECKOUT`

Payment initiation is allowed only when order status is `CHECKOUT`.

Check order:

```http
GET http://localhost:8080/api/orders/private/{orderId}
Authorization: Bearer <JWT_TOKEN>
```

If status is `PAYMENT_FAILED` or `PAID`, create/checkout a new order first.

## 4) Initiate Payment (`/pay`)

```http
POST http://localhost:8080/api/orders/private/{orderId}/pay
Authorization: Bearer <JWT_TOKEN>
```

Expected response includes:

- `paymentStatus: PROCESSING`
- `razorpayOrderId`
- `razorpayKeyId`
- `amountInPaise`

Save the returned `razorpayOrderId`.

## 5) Generate Signature in PowerShell

Use the same `razorpayOrderId` from `/pay`, any test `paymentId`, and your actual Razorpay key secret.

```powershell
$orderId = "order_xxx_from_pay"
$paymentId = "pay_manual_001"
$keySecret = "YOUR_ACTUAL_RAZORPAY_KEY_SECRET"

$data = "$orderId|$paymentId"
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($keySecret)
$hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($data))
$signature = -join ($hash | ForEach-Object { $_.ToString("x2") })
$signature
```

## 6) Verify Payment

### Option A: Direct to Payment Service (no gateway auth)

```http
POST http://localhost:8084/api/payments/internal/verify
Content-Type: application/json
```

```json
{
  "razorpayOrderId": "order_xxx_from_pay",
  "razorpayPaymentId": "pay_manual_001",
  "razorpaySignature": "generated_signature"
}
```

### Option B: Via Gateway (requires JWT)

```http
POST http://localhost:8080/api/payments/internal/verify
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

## 7) Confirm Final Order Status

```http
GET http://localhost:8080/api/orders/private/{orderId}
Authorization: Bearer <JWT_TOKEN>
```

Expected status after successful verify:

- `PAID`

## 8) Common Errors and Meaning

- `400 Invalid Razorpay payment signature`
  - `orderId`, `paymentId`, `signature`, or secret mismatch.
- `409 Order is not in CHECKOUT state`
  - Order cannot be paid from current status.
- `Missing Authorization Header` on `8080`
  - Gateway route requires JWT token.
- `503 Payment service is temporarily unavailable`
  - Service discovery/network/downstream issue.

## 9) Quick Retest Pattern

1. Create new order and move it to checkout.
2. Call `/pay`.
3. Generate signature with exact values.
4. Call `/verify`.
5. Check order status becomes `PAID`.

## 10) Security Note (Important)

Do not keep real Razorpay secrets hardcoded in source-controlled files.

Use environment variables only:

```yaml
razorpay:
  key-id: ${RAZORPAY_KEY_ID}
  key-secret: ${RAZORPAY_KEY_SECRET}
```

After testing, rotate any exposed test secrets.

