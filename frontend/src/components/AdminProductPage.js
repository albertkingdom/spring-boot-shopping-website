import { useEffect, useState } from "react";
import { Alert, Button, Container, Table } from "react-bootstrap";
import { MdDeleteOutline } from "react-icons/md";
import { useNavigate } from "react-router-dom";
import Pagination from "./subComponents/Pagination";
import { updateAccessToken } from "../util/refreshTokenUtil";

export default function AdminProductPage() {
  const navigate = useNavigate();
  const [productList, setProductList] = useState([]);
  const [totalPage, setTotalPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadProducts(0);
  }, []);

  function loadProducts(page = 0) {
    updateAccessToken(() => fetchProducts(page));
  }

  async function fetchProducts(page) {
    const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products?page=${page}`, {
      headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
    });
    if (!response.ok) {
      setError("無法讀取全站商品。");
      return;
    }
    const data = await response.json();
    if (data.totalPages > 0 && page >= data.totalPages) {
      return fetchProducts(data.totalPages - 1);
    }
    setProductList(data.content);
    setTotalPage(data.totalPages);
    setCurrentPage(page);
  }

  function deleteProduct(id) {
    updateAccessToken(async () => {
      const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/products/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
      });
      if (!response.ok) {
        setError("無法刪除商品。");
        return;
      }
      await fetchProducts(currentPage);
    });
  }

  return (
    <Container>
      <h2>全站商品</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      <Button className="mb-2" size="sm" variant="secondary" onClick={() => navigate("/admin/products/create")}>
        新增平台商品
      </Button>
      <Pagination totalPage={totalPage} currentPage={currentPage} fetch={loadProducts} />
      <Table striped bordered hover responsive>
        <thead><tr><th>ID</th><th>名稱</th><th>價格</th><th>操作</th></tr></thead>
        <tbody>
          {productList.map((product) => (
            <tr key={product.id}>
              <td>{product.id}</td><td>{product.name}</td><td>{product.price}</td>
              <td>
                <Button size="sm" variant="outline-secondary" className="me-2" onClick={() => navigate(`/admin/products/${product.id}`)}>修改</Button>
                <Button size="sm" variant="outline-danger" onClick={() => deleteProduct(product.id)}><MdDeleteOutline /></Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
}
