import { useEffect, useState } from "react";
import { Alert, Button, Container, Form, Table } from "react-bootstrap";
import { updateAccessToken } from "../util/refreshTokenUtil";

export default function SellerRoleManagement() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    updateAccessToken(fetchUsers);
  }, []);

  async function fetchUsers() {
    const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/user/all`, {
      headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
    });
    if (!response.ok) {
      setError("無法讀取使用者列表。請確認你仍具有平台管理員權限。");
      return;
    }
    setUsers(await response.json());
  }

  function changeSellerRole(user, method) {
    async function requestChange() {
      const response = await fetch(
        `${process.env.REACT_APP_BACKEND_URL}/api/admin/users/${user.id}/roles/seller`,
        {
          method,
          headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
        }
      );
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setError(body.message || "角色變更失敗。");
        return;
      }
      setError(null);
      fetchUsers();
    }
    updateAccessToken(requestChange);
  }

  const filteredUsers = users.filter((user) => {
    const keyword = searchTerm.trim().toLowerCase();
    return !keyword || user.email.toLowerCase().includes(keyword) || user.name.toLowerCase().includes(keyword);
  });

  return (
    <Container className="py-3">
      <h1>商家權限管理</h1>
      <p className="text-muted">授予角色後，該使用者需要重新登入，新的 access token 才會帶有 `ROLE_SELLER`。</p>
      {error && <Alert variant="danger">{error}</Alert>}
      <Form.Group className="mb-3" controlId="seller-user-search">
        <Form.Label>搜尋使用者</Form.Label>
        <Form.Control
          type="search"
          placeholder="依 email 或名稱搜尋"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />
      </Form.Group>
      <div className="table-responsive">
        <Table striped bordered hover>
          <thead>
            <tr><th>Email</th><th>名稱</th><th>目前角色</th><th>操作</th></tr>
          </thead>
          <tbody>
            {filteredUsers.map((user) => {
              const isSeller = user.roles.includes("ROLE_SELLER");
              return (
                <tr key={user.id}>
                  <td>{user.email}</td><td>{user.name}</td><td>{user.roles.join(", ")}</td>
                  <td>
                    <Button
                      size="sm"
                      variant={isSeller ? "outline-danger" : "primary"}
                      onClick={() => changeSellerRole(user, isSeller ? "DELETE" : "POST")}
                    >
                      {isSeller ? "撤銷商家" : "授予商家"}
                    </Button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      </div>
    </Container>
  );
}
