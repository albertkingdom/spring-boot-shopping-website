import jwt_decode from "jwt-decode";

export async function updateAccessToken(complete) {
  let tokenExpireAt = sessionStorage.getItem("token_expireAt");
  let refreshToken = sessionStorage.getItem("refresh_token");
  // if expire time is 10s from expiration
  if (new Date() >= parseInt(tokenExpireAt) - 1000 * 10) {
    console.log("will get new access token");
 
    const resp = await fetch(`${process.env.REACT_APP_BACKEND_URL}/api/refreshToken`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${refreshToken}`,
      },
    });
    if (!resp.ok) {
      throw new Error("Unable to refresh access token");
    }
    const data = await resp.json();
    let accessToken = data["access_token"];
    if (!accessToken) {
      throw new Error("Refresh response did not contain an access token");
    }
    sessionStorage.setItem("access_token", accessToken);
    let decodedToken = jwt_decode(accessToken);
    const { exp } = decodedToken;

    sessionStorage.setItem("token_expireAt", exp * 1000);
    console.log("successfully get new access token");
    return complete();
  } else {
    console.log("no need to get new token");
    return complete();
  }
}
