import { useEffect, useState } from "react";
import { Alert, Container, Table, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { MdDeleteOutline } from "react-icons/md";
import { updateAccessToken } from "../util/refreshTokenUtil";
import Pagination from "./subComponents/Pagination";

function ProductPageForSeller({ refreshAccessToken = updateAccessToken }) {
  let navigate = useNavigate();
  const [productList, setProductList] = useState([]);
  const [totalPage, setTotalPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(0); // page start from 0
  const [error, setError] = useState(null);

  useEffect(() => {
    loadProducts(0);
  }, []);
  async function loadProducts(page = 0) {
    try {
      await refreshAccessToken(() => fetchProducts(page));
    } catch (refreshError) {
      setError("登入已失效，請重新登入後再試。");
    }
  }
  async function fetchProducts(page) {
    try {
      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/seller/products?page=${page}`, {
      headers: {
        Authorization: `Bearer ${sessionStorage.getItem("access_token")}`,
      },
      });
      if (!response.ok) {
        setError("無法讀取我的商品，請重新登入後再試。");
        return;
      }
      const data = await response.json();
      const effectiveTotalPages = Math.max(data.totalPages, 1);
      if (page >= effectiveTotalPages) {
        return fetchProducts(effectiveTotalPages - 1);
      }
      setProductList(data.content);
      setTotalPage(effectiveTotalPages);
      setCurrentPage(page);
      setError(null);
    } catch (requestError) {
      setError("無法讀取我的商品，請重新登入後再試。");
    }
  }
  async function handleDelete(id) {
    async function deleteProduct() {
      let accessToken = sessionStorage.getItem("access_token");

      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products/${id}`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      });
      if (!response.ok) {
        throw new Error("無法刪除商品");
      }
    }
    try {
      await refreshAccessToken(deleteProduct);
      await fetchProducts(currentPage);
    } catch (refreshError) {
      setError(refreshError.message === "Unable to refresh access token" || refreshError.message === "Refresh response did not contain an access token"
        ? "登入已失效，請重新登入後再試。"
        : "無法刪除商品，請稍後再試。");
    }
  }
  return (
    <Container>
      {error && <Alert variant="danger">{error}</Alert>}
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
        fetch={loadProducts} />

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
                  aria-label={`刪除商品 ${product.name}`}
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
