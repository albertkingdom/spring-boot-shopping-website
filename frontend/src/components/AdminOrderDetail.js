import { useEffect, useState } from "react";
import { Alert, Button, Container, Table } from "react-bootstrap";
import { useNavigate, useParams } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";

export default function AdminOrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    updateAccessToken(async () => {
      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order/${id}`, {
        headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
      });
      if (!response.ok) {
        setError("無法讀取這筆訂單。");
        return;
      }
      setOrder(await response.json());
    });
  }, [id]);

  if (error) return <Container className="py-3"><Alert variant="danger">{error}</Alert></Container>;
  if (!order) return <Container className="py-3">載入訂單中…</Container>;

  return (
    <Container>
      <h2>訂單 #{order.id}</h2>
      <p><strong>買家：</strong>{order.userEmail}</p>
      <p><strong>訂單總額：</strong>{order.priceSum}</p>
      <Table striped bordered responsive>
        <thead><tr><th>商品</th><th>數量</th><th>單價</th></tr></thead>
        <tbody>{order.items.map((item, index) => <tr key={`${item.productName}-${index}`}><td>{item.productName}</td><td>{item.quantity}</td><td>{item.unitPrice}</td></tr>)}</tbody>
      </Table>
      <Button variant="outline-secondary" onClick={() => navigate("/admin/orders")}>返回訂單列表</Button>
    </Container>
  );
}
