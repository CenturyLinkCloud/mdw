const statuses = {
  process: [
    'Pending',
    'In Progress',
    'Failed',
    'Completed',
    'Canceled',
    'Waiting'
  ],
  activity: [
    'In Progress', 
    'Failed', 
    'Waiting'
  ],
  request: [
    '200 - OK',
    '201 - Created',
    '202 - Accepted',
    '400 - Bad Request',
    '401 - Unauthorized',
    '403 - Forbidden',
    '404 - Not Found',
    '405 - Method Not Allowed',
    '409 - Conflict',
    '500 - Internal Server Error',
    '501 - Not Implemented',
    '502 - Bad Gateway',
    '503 - Service Unavailable'
  ]    
};

export default statuses;