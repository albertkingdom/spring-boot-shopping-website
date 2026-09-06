import { useState, useEffect } from "react";
import { Container, Table, Button } from "react-bootstrap";
import { Link, useNavigate } from "react-router-dom";
import { MdDeleteOutline } from "react-icons/md";
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
    console.log(`start to fetch order page ${page}`);
    let accessToken = sessionStorage.getItem("access_token");

    fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order?page=${page}`, {
      // credentials: "include",
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
  function handleDelete(id) {
    function deleteOrder() {
      let accessToken = sessionStorage.getItem("access_token");

      fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order/${id}`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      })
        .then((response) => response.text())
        .then((data) => {
          console.log(data);

          //navigate("/product")
          setOrderList(orderList.filter((order) => order.id !== id));
        });
    }
    updateAccessToken(deleteOrder);
  }
  // function onClickNextPage(e) {
  //   e.preventDefault();
  //   if (currentPage + 1 === totalPage) {
  //     return
  //   }
  //   fetchOrder(currentPage + 1)
  // }
  // function onClickPreviousPage(e) {
  //   e.preventDefault();
  //   if (currentPage === 0) {
  //     return
  //   }
  //   fetchOrder(currentPage - 1)
  // }
  // function onClickPage(e, pageVisable) {
  //   e.preventDefault();
  //   fetchOrder(pageVisable - 1)
  // }
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
            {/* {isDeleteMode ? <td>delete</td> : null} */}
            <th>id</th>
            <th>userId</th>
            <th>order price</th>
            <th>訂單成立時間</th>
            <th>Edit</th>
          </tr>
        </thead>
        <tbody>
          {orderList.map((order) => (
            <tr key={order.id}>

              <td>{order.id}</td>
              <td>{order.userId}</td>
              <td>{order.priceSum}</td>
              <td>{formatDate(order.createdAt)}</td>
              <td>
                <Button
                  variant="outline-secondary"
                  onClick={() => navigate(`/seller/editOrder/${order.id}`)}
                  className="me-2"
                >
                  Edit
                </Button>
                <Button
                  variant="outline-danger"
                  onClick={() => handleDelete(order.id)}
                >
                  <MdDeleteOutline />
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
}
