import { useEffect, useState } from "react";
import { Container, Table, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { MdDeleteOutline } from "react-icons/md";
import { updateAccessToken } from "../util/refreshTokenUtil";
import Pagination from "./subComponents/Pagination";

function ProductPageForSeller() {
  let navigate = useNavigate();
  const [productList, setProductList] = useState([]);
  const [totalPage, setTotalPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(0); // page start from 0

  useEffect(() => {
    fetchProduct(0)
    updateAccessToken(fetchProduct);

  }, []);
  function fetchProduct(page = 0) {
    fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products?page=${page}`, {
      // credentials: "include",
    })
      .then((response) => response.json())
      .then((data) => {
        setProductList(data.content)
        setTotalPage(data.totalPages)
        setCurrentPage(page)
      }
      );
  }
  function handleDelete(id) {
    function deleteProduct() {
      let accessToken = sessionStorage.getItem("access_token");

      fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products/${id}`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      })
        .then((response) => response.text())
        .then((data) => {
          console.log(data);

          setProductList(productList.filter((product) => product.id !== id));
        });
    }
    updateAccessToken(deleteProduct);
  }
  return (
    <Container>
      <div className="">
        <Button
          role="link"
          size="sm"
          variant="secondary"
          className="m-2"
          onClick={() => navigate("/seller/createProduct")}
        >
          Create
        </Button>
        {/* <Button
          size="sm"
          variant="secondary"
          onClick={() => setIsDeleteMode(!isDeleteMode)}
        >
          Delete Mode
        </Button> */}
      </div>
      <Pagination
        totalPage={totalPage}
        currentPage={currentPage}
        fetch={fetchProduct} />

      <Table striped bordered hover>
        <thead>
          <tr>
            {/* {isDeleteMode ? <th>Delete</th> : null} */}
            <th>id</th>
            <th>name</th>
            <th>price</th>
            <th>Edit</th>
          </tr>
        </thead>
        <tbody>
          {productList.map((product) => (
            <tr key={product.id}>
              {/* {isDeleteMode ? (
                <td>
                  <Button
                    variant="light"
                    onClick={() => handleDelete(product.id)}
                  >
                    <MdDeleteOutline />
                  </Button>
                </td>
              ) : null} */}
              <td>{product.id}</td>
              <td>{product.name}</td>
              <td>{product.price}</td>
              <td>
                <Button
                  variant="outline-secondary"
                  onClick={() => navigate(`/seller/editProduct/${product.id}`)}
                  className="me-2"
                >
                  Edit
                </Button>
                <Button
                  variant="outline-danger"
                  onClick={() => handleDelete(product.id)}
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

export default ProductPageForSeller;
