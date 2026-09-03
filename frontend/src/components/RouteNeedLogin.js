import { Route, Link, Navigate } from "react-router-dom";
import { useState, useEffect } from "react";

/*
* To check the login status and keep <route> element as children or redirect to login page
*/

export default function RouteNeedLogin({ children, redirectTo, userName }) {
  // if userName != null ==> user has logged in
  return userName ? children : <Navigate to={redirectTo} />;
}
