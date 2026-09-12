import { useState, useEffect } from "react";
import { Alert, Container, Table, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";
import Pagination from "./subComponents/Pagination";

export default function OrderListPageForSeller({ refreshAccessToken = updateAccessToken }) {
  let navigate = useNavigate();
  const [orderList, setOrderList] = useState([]);
  const [totalPage, setTotalPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(0); // page start from 0
  const [error, setError] = useState(null);

  useEffect(() => {
    loadOrders(0);
  }, []);

  async function loadOrders(page = 0) {
    try {
      await refreshAccessToken(() => fetchOrder(page));
    } catch {
      setError("登入已失效，請重新登入後再試。");
    }
  }

  async function fetchOrder(page = 0) {
    try {
      const accessToken = sessionStorage.getItem("access_token");
      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/seller/orders?page=${page}`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      });
      if (!response.ok) {
        setError(response.status === 403 ? "你沒有查看商家訂單的權限。" : "無法讀取我的訂單，請稍後再試。");
        return;
      }
      const data = await response.json();
      setOrderList(data.content || []);
      setTotalPage(Math.max(data.totalPages || 0, 1));
      setCurrentPage(page);
      setError(null);
    } catch {
      setError("無法讀取我的訂單，請稍後再試。");
    }
  }
  function formatDate(dateStr) {
    if (!dateStr) {
      return "—";
    }
    let dateObj = new Date(dateStr)
    let year = dateObj.getFullYear()
    let month = dateObj.getMonth() + 1
    let date = dateObj.getDate()
    let hourStr = dateObj.getHours() < 10 ? `0${dateObj.getHours()}` : `${dateObj.getHours()}`
    let minStr = dateObj.getMinutes() < 10 ? `0${dateObj.getMinutes()}` : `${dateObj.getMinutes()}`
    let secStr = dateObj.getSeconds() < 10 ? `0${dateObj.getSeconds()}` : `${dateObj.getSeconds()}`
    return `${year}-${month}-${date} ${hourStr}:${minStr}:${secStr}`
  }
  return (
    <Container>
      {error && <Alert variant="danger">{error}</Alert>}
      <Pagination
        totalPage={totalPage}
        currentPage={currentPage}
        fetch={loadOrders} />
      {orderList.length === 0 && !error ? (
        <Alert variant="info" role="status">
          <Alert.Heading>目前沒有訂單</Alert.Heading>
          <p>當你的商品售出後，相關訂單會顯示在這裡。</p>
          <Button variant="outline-primary" onClick={() => navigate("/seller/product_list_page_seller")}>
            查看我的商品
          </Button>
        </Alert>
      ) : (
        <div className="table-responsive">
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>id</th>
                <th>買家 Email</th>
                <th>本店小計</th>
                <th>訂單成立時間</th>
                <th>詳情</th>
              </tr>
            </thead>
            <tbody>
              {orderList.map((order) => (
                <tr key={order.id}>
                  <td>{order.id}</td>
                  <td>{order.buyerEmail}</td>
                  <td>{order.sellerSubtotal}</td>
                  <td>{formatDate(order.createdAt)}</td>
                  <td>
                    <Button
                      variant="outline-secondary"
                      onClick={() => navigate(`/seller/orders/${order.id}`)}
                    >
                      查看
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      )}
    </Container>
  );
}
