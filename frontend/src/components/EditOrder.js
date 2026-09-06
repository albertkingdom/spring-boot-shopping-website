import { useEffect, useState } from "react";
import { Form, Button, Container } from "react-bootstrap";
import { useNavigate, useParams } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";

function EditOrder() {
  let navigate = useNavigate();
  let { id } = useParams();

  const [userId, setUserId] = useState();
  const [userEmail, setUserEmail] = useState();
  const [orderPrice, setOrderPrice] = useState(0);
  const [orderItems, setOrderItems] = useState([]);

  function handleSubmit(e) {
    e.preventDefault();

    function deleteOrder() {
      let accessToken = sessionStorage.getItem("access_token");
      fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order/${id}`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      })
        .then((response) => {
          if (response.status === 200) {
            navigate("/seller/order_list_page_seller");
          } else {
            throw new Error("delete order failer");
          }
        })
        .catch((error) => console.log(error));
    }
    updateAccessToken(deleteOrder);
  }
  useEffect(() => {
    function fetchOrderById() {
      let accessToken = sessionStorage.getItem("access_token");
      fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order/${id}`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      })
        .then((response) => response.json())
        .then((data) => {
          // console.log(data)
          setUserId(data.userId);
          setUserEmail(data.userEmail)
          setOrderPrice(data.priceSum);
          setOrderItems(data.orderItemDetailList);

        });
    }
    updateAccessToken(fetchOrderById);
  }, [id]);
  return (
    <Container>
      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3">
          <Form.Label>Order Id</Form.Label>
          <Form.Control type="text" readOnly value={id} />
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>User Email</Form.Label>
          <Form.Control type="text" value={userEmail} readOnly />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Price</Form.Label>
          <Form.Control type="text" value={orderPrice} readOnly />
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>訂單商品</Form.Label>

          {orderItems.map((item) => (
            <p className="form-control" key={item.productName} readOnly>
              名稱: {item.productName}, 商品數量: {item.quantity}, 單價:
              {item.productPrice}
            </p>
          ))}
        </Form.Group>
        <Button variant="primary" type="submit">
          Delete
        </Button>
      </Form>
    </Container>
  );
}

export default EditOrder;
