response {
  '_type' 'com.centurylink.mdw.model.StatusResponse'
  statusResponse {
    status {
      code (serviceResponse.status.code == 0 ? 200 : serviceResponse.status.code)
      message serviceResponse.status.message
    }
  }
}
