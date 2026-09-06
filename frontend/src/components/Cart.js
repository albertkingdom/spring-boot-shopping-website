import { useEffect, useState } from "react";
import { Container, Table, Button, Accordion, Alert } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";

function Cart({ setCartCount }) {
  let navigate = useNavigate();
  const [cartList, setCartList] = useState([]);
  const [isDeleteMode, setIsDeleteMode] = useState(false);
  const [cartTotalPrice, setCartTotalPrice] = useState(0);
  const [show, setShow] = useState(false);

  useEffect(() => {
    getLocalStorage();
  }, []);
  useEffect(() => {
    function calTotalPrice() {
      let totalPrice = 0;
      let totalCount = 0;
      cartList.forEach((item) => {
        totalPrice += item.totalPrice;
        totalCount += item.count;
      });
      //console.log("cart all price", totalPrice)
      setCartTotalPrice(totalPrice);
      setCartCount(totalCount);
    }
    calTotalPrice();
  }, [cartList, setCartCount]);
  async function getLocalStorage() {
    let existing = localStorage.getItem("shopping_cart");
    let cartArray = JSON.parse(existing); //[ {id:1, count:3},{...}]
    let cartArrayWithPriceAndCount = [];
    if (cartArray == null) {
      return;
    }
    // fetch updated product price, name from server
    for (let i = 0; i < cartArray.length; i++) {
      let productInfoResponse = await fetch(
        `${process.env.REACT_APP_BACKEND_URL}/api/products/${cartArray[i].id}`
      );
      let productInfo = await productInfoResponse.json();
      // console.log(productInfo);

      cartArrayWithPriceAndCount.push({
        id: cartArray[i].id,
        name: productInfo.name,
        price: productInfo.price,
        count: cartArray[i].count,
        totalPrice: productInfo.price * cartArray[i].count,
      });
    }
    //console.log("cartArrayWithPriceAndCount", cartArrayWithPriceAndCount);
    setCartList(cartArrayWithPriceAndCount);
  }

  function handleDeleteItem(productId) {
    let updatedCartList = cartList.filter((product) => product.id !== productId);
    setCartList(updatedCartList);

    localStorage.setItem("shopping_cart", JSON.stringify(updatedCartList));
  }
  function checkIfLogin() {
    let accessToken = sessionStorage.getItem("access_token");
    accessToken == null ? setShow(true) : setShow(false);
    return accessToken != null
  }
  function handleSubmit() {
    if (!checkIfLogin()) return;
    let orderBody = {
      items: cartList.map((item) => {
        return { productId: item.id, productCount: item.count };
      }),
    };

    function sumbitOrder() {
      let accessToken = sessionStorage.getItem("access_token");
      fetch(`${process.env.REACT_APP_BACKEND_URL}/api/order`, {
        method: "POST",
        body: JSON.stringify(orderBody),
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        // credentials: "include",
      })
        .then((response) => {
          if (response.status === 200) {
            localStorage.removeItem("shopping_cart");
            setCartCount(0);
            setCartList([])
            navigate("/product_list");
          } else {
            throw new Error("Submit Order Error");
          }
        })
        .catch((error) => console.log(error));
    }
    updateAccessToken(sumbitOrder);
  }

  return (
    <Container className="py-3">
      {show &&
        <Alert variant="danger" onClose={() => setShow(false)} dismissible>
          <p>
            請先登入!
          </p>
        </Alert>
      }
      <Accordion defaultActiveKey="0">
        <Accordion.Item eventKey="0">
          <Accordion.Header>購物車清單</Accordion.Header>
          <Accordion.Body>
            <Table striped bordered hover>
              <thead>
                <tr className="my-3">
                  {isDeleteMode ? <td>delete</td> : null}
                  <th>商品</th>
                  <th>單價</th>
                  <th>數量</th>
                  <th>總計</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {cartList.map((product) => (
                  <tr className="my-3" key={product.id}>
                    {isDeleteMode ? (
                      <td>
                        <Button variant="light"></Button>
                      </td>
                    ) : null}

                    <td>{product.name}</td>
                    <td>{product.price}</td>
                    <td>{product.count}</td>
                    <td>{product.totalPrice}</td>

                    <th>
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => handleDeleteItem(product.id)}
                      >
                        刪除
                      </Button>
                    </th>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Accordion.Body>
        </Accordion.Item>
        <Accordion.Item eventKey="1">
          <Accordion.Header>付款</Accordion.Header>
          <Accordion.Body>
            <div>
              <div>
                <input
                  type="radio"
                  id="contactChoice1"
                  name="contact"
                  value="email"
                />
                <label htmlFor="contactChoice1">花旗銀行</label>
              </div>
              <div>
                <input
                  type="radio"
                  id="contactChoice2"
                  name="contact"
                  value="phone"
                />
                <label htmlFor="contactChoice2">台新銀行</label>
              </div>
            </div>
          </Accordion.Body>
        </Accordion.Item>
      </Accordion>
      <div className="d-flex justify-content-end my-3">
        <h5>總金額:${cartTotalPrice}</h5>
        <Button className="" onClick={handleSubmit}>
          下訂單
        </Button>
      </div>

    </Container>
  );
}

export default Cart;
