import { useEffect, useState } from "react";
import { Form, Button, Container } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";

function CreateProduct() {
  let navigate = useNavigate();
  const [productName, setProductName] = useState("");
  const [productPrice, setProductPrice] = useState(0);
  const [formFormatError, setFormFormatError] = useState({});
  const [productNameFormatError, setproductNameFormatError] = useState(null)
  const [productPriceFormatError, setproductPriceFormatError] = useState(null)

  function handleSubmit(e) {
    e.preventDefault();
    console.log("handle submit", e.target);
    console.log(
      "handle submit",
      e.target.productName.value,
      e.target.productPrice.value,
      e.target.image.files[0]
    );

    async function createProduct(data) {
      let accessToken = sessionStorage.getItem("access_token");
      const formData = new FormData();
      formData.append("productName", e.target.productName.value);
      formData.append("productPrice", e.target.productPrice.value);
      formData.append("productImage", e.target.image.files[0]);

      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products`, {
        method: "POST",
        body: formData,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        }
      })
      if (response.status === 200) {
        const data = await response.json()
        console.log(data);
        if (data.id != null) {
          navigate("/seller/product_list_page_seller");
        }
      }
      if (response.status === 400) {
        const data = await response.json()
        console.log(data)
        setFormFormatError(data)
        handleFormatError(data)
      }

    }

    updateAccessToken(createProduct);
  }

  function handleFormatError(errorMessage) {
    if (errorMessage) {
      console.log("formFormatError", errorMessage)
      let productNameError = errorMessage.errors.filter(error => error.field?.includes("productName") || error.param?.includes("productName"))
      let productPriceError = errorMessage.errors.filter(error => error.field?.includes("productPrice")|| error.param?.includes("productPrice"))
      if (productNameError.length > 0) {
        setproductNameFormatError(productNameError.map(error => error.message || error.msg))
      }
      if (productPriceError.length > 0) {
        setproductPriceFormatError(productPriceError.map(error => error.message || error.msg))
      }
    }
  }
  return (
    <Container>

      <Form onSubmit={handleSubmit} encType="multipart/form-data">
        <Form.Group className="mb-3" controlId="formBasicEmail">
          <Form.Label>Name</Form.Label>
          <Form.Control
            type="text"
            name="productName"
            placeholder="Enter name"
            required
            value={productName}
            onChange={(e) => setProductName(e.target.value)}
          />
          {productNameFormatError && (
            <Form.Text style={{ color: "red" }}>
              product name {productNameFormatError.join(";")}
            </Form.Text>
          )}
        </Form.Group>

        <Form.Group className="mb-3" controlId="formBasicPassword">
          <Form.Label>Price</Form.Label>
          <Form.Control
            type="text"
            name="productPrice"
            placeholder="Price"
            required
            value={productPrice}
            onChange={(e) => setProductPrice(e.target.value)}
          />
          {productPriceFormatError && (
            <Form.Text style={{ color: "red" }}>
              product price {productPriceFormatError.join("")}
            </Form.Text>
          )}
        </Form.Group>
        <Form.Group controlId="formFile" className="mb-3">
          <Form.Label>Product Image</Form.Label>
          <Form.Control name="image" type="file" />
        </Form.Group>
        <Button variant="primary" type="submit">
          Submit
        </Button>
      </Form>
    </Container>
  );
}

export default CreateProduct;
