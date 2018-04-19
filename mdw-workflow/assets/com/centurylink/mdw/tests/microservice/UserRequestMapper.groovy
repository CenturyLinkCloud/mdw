serviceRequest {
  cuid request.user.id
  name request.user.firstName + request.user.lastName
  attributes {
    email request.user.emailAddress
  }
}
