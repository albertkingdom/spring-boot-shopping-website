import { useState, useEffect } from "react";
import { Container, Button, Form, ButtonGroup, Alert } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import jwt_decode from "jwt-decode";
import styles from "../style/Login.module.css"

function Login({ userName, setUser, setRole }) {
  let navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");

  const [loginError, setLoginError] = useState(null); //login error msg
  const [registerError, setRegisterError] = useState(null); // register error msg
  const [loginForm, setLoginForm] = useState(true);
  useEffect(() => {
    //clear error when switch form
    setLoginError(null)
    setRegisterError(null)
    setEmail("")
    setPassword("")
    setName("")
  }, [loginForm])

  async function handleLogIn(e) {
   
    e.preventDefault();

    const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/login`, {
      method: "POST",
      body: JSON.stringify({ email: email, password: password }),
      headers: {
        "Content-Type": "application/json",
      },
    })
    if (response.status === 200) {
      const data = await response.json()
      console.log("login data", data);
      setUser(data.username);
      let accessToken = data["access_token"];
      let refreshToken = data["refresh_token"];
      sessionStorage.setItem("shopping-website-user", data.username);
      sessionStorage.setItem("access_token", accessToken);
      sessionStorage.setItem("refresh_token", refreshToken);

      let decodedToken = jwt_decode(accessToken);
      const { roles, exp } = decodedToken;
      console.log(decodedToken);
      setRole(roles); // ["ROLE_USER"]
      sessionStorage.setItem("token_expireAt", exp * 1000);
      navigate("/product_list");
    } else {
      const errMsg = await response.text()
      console.error(errMsg)
      setLoginError(errMsg)
    }

  }
  async function handleRegister(e) {
    e.preventDefault();

    const response = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/register`, {
      method: "POST",
      body: JSON.stringify({ email: email, password: password, name: name }),
      headers: {
        "Content-Type": "application/json",
      },
    })
    const data = await response.json();
    if (response.status === 200) {

      console.log("register success", data)
      setEmail("")
      setPassword("")
      setName("")
      setLoginForm(true)
    } else {
      console.error("register failed", data)
      setRegisterError(data)
    }

  }
  function handleLogout() {
    setUser(null);
    setRole([]);
    sessionStorage.removeItem("shopping-website-user");
    sessionStorage.removeItem("access_token");
  }
  

  if (userName != null) {
    return (
      <Container>
        <h1>Hello, you have logged in as {userName}</h1>
        <Button className="" onClick={handleLogout}>
          Log out
        </Button>
      </Container>
    );
  }

  return (
    <div className={`${styles.container}`}>
      <ButtonGroup className={`${styles.customButtonGroup}`}>
        <Button
          variant={loginForm ? "primary" : "outline-secondary"}
          onClick={() => setLoginForm(true)}
        >
          登入
        </Button>
        <Button
          variant={!loginForm ? "primary" : "outline-secondary"}
          onClick={() => setLoginForm(false)}
        >
          註冊
        </Button>
      </ButtonGroup>
      {loginForm && loginError &&
        <Alert variant="danger" style={{ width: "fit-content", margin: "10px auto" }}>
          {loginError}!
        </Alert>
      }
      {!loginForm && registerError &&
        <Alert variant="danger" style={{ width: "fit-content", margin: "10px auto" }}>
          {registerError.errors.map(error=>`${error.message || error.msg}`).join(" ")}
        </Alert>
      }
      <Form onSubmit={loginForm ? handleLogIn : handleRegister} className="w-50 mx-auto color-white">
        <Form.Group className="mb-3" controlId="formBasicEmail">
          <Form.Label>Email</Form.Label>
          <Form.Control
            type="email"
            placeholder="Enter email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Form.Group>

        <Form.Group className="mb-3" controlId="formBasicPassword">
          <Form.Label>Password</Form.Label>
          <Form.Control
            type="password"
            placeholder={loginForm ? "Password" : "Password at least 6 characters."}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

        </Form.Group>
        {!loginForm && (
          <Form.Group className="mb-3" controlId="formBasicEmail">
            <Form.Label>Name</Form.Label>
            <Form.Control
              type="text"
              placeholder="Enter name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </Form.Group>
        )}
        {loginForm && (
          <div className="d-grid">
            <Button variant="secondary" type="submit">
              登入
            </Button>
          </div>
        )}
        {!loginForm && (
          <div className="d-grid">
            <Button variant="secondary" type="submit">
              註冊
            </Button>
          </div>
        )}
      </Form>
    </div>
  );
}

export default Login;
