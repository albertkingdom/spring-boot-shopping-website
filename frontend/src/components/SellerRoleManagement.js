import { useEffect, useState } from "react";
import { Alert, Button, Container, Form, Modal, Table } from "react-bootstrap";
import { Link } from "react-router-dom";
import { updateAccessToken } from "../util/refreshTokenUtil";

export default function SellerRoleManagement({ refreshAccessToken = updateAccessToken }) {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [pendingChange, setPendingChange] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    refreshAccessToken(fetchUsers);
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

  function requestRoleChange(user, method) {
    setPendingChange({ user, method });
    setError(null);
    setSuccess(null);
  }

  async function confirmRoleChange() {
    if (!pendingChange) {
      return;
    }

    const { user, method } = pendingChange;
    setIsSubmitting(true);
    try {
      await refreshAccessToken(async () => {
        const response = await fetch(
          `${process.env.REACT_APP_BACKEND_URL}/api/admin/users/${user.id}/roles/seller`,
          {
            method,
            headers: { Authorization: `Bearer ${sessionStorage.getItem("access_token")}` },
          }
        );
        if (!response.ok) {
          const body = await response.json().catch(() => ({}));
          if (response.status === 409) {
            setError(
              <>
                撤銷失敗：該使用者仍擁有商品，請先移轉或下架商品。{" "}
                <Link to="/admin/products">前往全站商品</Link>
              </>
            );
          } else {
            setError(body.message || "角色變更失敗。");
          }
          return;
        }
        setError(null);
        setSuccess(`${user.email} 的商家角色已${method === "POST" ? "授予" : "撤銷"}，請重新登入後生效。`);
        await fetchUsers();
      });
    } catch {
      setError("登入已失效，請重新登入後再試。");
    } finally {
      setIsSubmitting(false);
      setPendingChange(null);
    }
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
      {success && <Alert variant="success">{success}</Alert>}
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
                      onClick={() => requestRoleChange(user, isSeller ? "DELETE" : "POST")}
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
      <Modal
        show={Boolean(pendingChange)}
        onHide={() => !isSubmitting && setPendingChange(null)}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>確認變更商家角色</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {pendingChange && (
            <>
              確認要對 <strong>{pendingChange.user.email}</strong>{" "}
              {pendingChange.method === "POST" ? "授予" : "撤銷"}商家角色嗎？
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setPendingChange(null)} disabled={isSubmitting}>
            取消
          </Button>
          <Button variant="primary" onClick={confirmRoleChange} disabled={isSubmitting}>
            {isSubmitting ? "處理中…" : "確認變更"}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}
