module.exports = {
  process: {
    Pending: {
      color: '#0099C6'
    },
    'In Progress': {
      color: '#109618'
    },
    Failed: {
      color: '#DC3912'
    },
    Completed: {
      color: '#5574A6'
    },
    Canceled: {
      color: '#990099'
    },
    Waiting: {
      color: '#FF9900'
    }
  },
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
  ],
  task: [
    'Open', 
    'Assigned', 
    'In Progress', 
    'Completed', 
    'Canceled'
  ]    
};