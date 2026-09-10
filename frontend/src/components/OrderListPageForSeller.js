import { useState, useEffect } from "react";
import { Container, Table, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";
import Pagination from "./subComponents/Pagination";

export default function OrderListPageForSeller() {
  let navigate = useNavigate();
  const [orderList, setOrderList] = useState([]);
  const [totalPage, setTotalPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(0); // page start from 0

  useEffect(() => {
    fetchOrder(0)
    updateAccessToken(fetchOrder);
  }, []);
  function fetchOrder(page = 0) {
    let accessToken = sessionStorage.getItem("access_token");

    fetch(`${process.env.REACT_APP_BACKEND_URL}/api/seller/orders?page=${page}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
    })
      .then((response) => response.json())
      .then((data) => {
        setOrderList(data.content)
        setTotalPage(data.totalPages)
        setCurrentPage(page)
      }
      );
  }
  function formatDate(dateStr) {
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

      <Pagination
        totalPage={totalPage}
        currentPage={currentPage}
        fetch={fetchOrder} />
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
    </Container>
  );
}
