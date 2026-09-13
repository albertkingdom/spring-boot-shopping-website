import { useEffect, useState } from "react";
import { Alert, Button, Container, Table } from "react-bootstrap";
import { MdDeleteOutline } from "react-icons/md";
import { useNavigate } from "react-router-dom";
import Pagination from "./subComponents/Pagination";
import { updateAccessToken } from "../util/refreshTokenUtil";

export default function AdminOrderList() {
  const navigate = useNavigate();
  const [orderList, setOrderList] = useState([]);
  const [totalPage, setTotalPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadOrders(0);
  }, []);

  function loadOrders(page = 0) {
    updateAccessToken(() => fetchOrders(page));
  }

  async function fetchOrders(page) {
    const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order?page=${page}`, {
      headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
    });
    if (!response.ok) {
      setError("無法讀取全站訂單。");
      return;
    }
    const data = await response.json();
    if (data.totalPages > 0 && page >= data.totalPages) {
      return fetchOrders(data.totalPages - 1);
    }
    setOrderList(data.content);
    setTotalPage(data.totalPages);
    setCurrentPage(page);
  }

  function deleteOrder(id) {
    updateAccessToken(async () => {
      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
      });
      if (!response.ok) {
        setError("無法刪除訂單。");
        return;
      }
      await fetchOrders(currentPage);
    });
  }

  return (
    <Container>
      <h2>全站訂單</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      <Pagination totalPage={totalPage} currentPage={currentPage} fetch={loadOrders} />
      <Table striped bordered hover responsive>
        <thead><tr><th>ID</th><th>買家 ID</th><th>訂單總額</th><th>成立時間</th><th>操作</th></tr></thead>
        <tbody>
          {orderList.map((order) => (
            <tr key={order.id}>
              <td>{order.id}</td><td>{order.userId}</td><td>{order.priceSum}</td><td>{order.createdAt || "-"}</td>
              <td>
                <Button size="sm" variant="outline-secondary" className="me-2" onClick={() => navigate(`/admin/orders/${order.id}`)}>查看</Button>
                <Button size="sm" variant="outline-danger" onClick={() => deleteOrder(order.id)}><MdDeleteOutline /></Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
}
