import { useEffect, useState } from "react";
import { Alert, Container, Table } from "react-bootstrap";
import { useParams } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";

export default function SellerOrderDetail() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchOrder() {
      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/seller/orders/${id}`, {
        headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
      });
      if (!response.ok) {
        setError("無法讀取這筆訂單，或訂單不屬於你的商店。");
        return;
      }
      setOrder(await response.json());
    }
    updateAccessToken(fetchOrder);
  }, [id]);

  if (error) {
    return <Container className="py-3"><Alert variant="danger">{error}</Alert></Container>;
  }
  if (!order) {
    return <Container className="py-3">載入訂單中…</Container>;
  }
  return (
    <Container>
      <h2>訂單 #{order.id}</h2>
      <p><strong>買家 Email：</strong>{order.buyerEmail}</p>
      <p><strong>本店小計：</strong>{order.sellerSubtotal}</p>
      <div className="table-responsive">
        <Table striped bordered>
          <thead><tr><th>商品</th><th>數量</th><th>單價</th></tr></thead>
          <tbody>
            {order.items.map((item, index) => (
              <tr key={`${item.productName}-${index}`}>
                <td>{item.productName}</td><td>{item.quantity}</td><td>{item.unitPrice}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>
    </Container>
  );
}
