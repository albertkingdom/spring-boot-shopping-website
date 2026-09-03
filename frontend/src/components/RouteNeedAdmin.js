import { Route, Link, Navigate } from "react-router-dom";


/*
 * To check the login status and keep <route> element as children or redirect to login page
 */

export default function RouteNeedAdmin({
  children,
  redirectTo,
  userRole,
  userName,
}) {
  // if userName != null ==> user has logged in
  
  if (userName && userRole.indexOf("ROLE_ADMIN") > -1) {
    return children;
  } else if (userName && userRole.indexOf("ROLE_ADMIN") === -1) {
    return <h1>You don't have permission in admin page.</h1>;
  } else {
    return <Navigate to={redirectTo} />;
  }
}
