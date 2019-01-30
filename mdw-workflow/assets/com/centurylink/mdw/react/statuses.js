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
  activity: {
    'In Progress': {
      color: '#109618'
    },
    Failed: {
      color: '#DC3912'
    },
    Waiting: {
      color: '#FF9900'
    }
  },
  task: {
    Open: {
      color: '#22AA99'
    }, 
    Assigned: {
      color: '#FF9900'
    }, 
    'In Progress': {
      color: '#109618'
    }, 
    Completed: {
      color: '#5574A6'
    }, 
    Canceled: {
      color: '#990099'
    }
  },
  request: {
    200: {
      message: 'OK',
      color: '#109618'
    },
    201: {
      message: 'Created',
      color: '#5574A6'
    },
    202: {
      message: 'Accepted',
      color: '#3366CC'
    },
    400: {
      message: 'Bad Request',
      color: '#DD4477'
    },
    401: {
      message: 'Unauthorized',
      color: '#8B0707'
    },
    403: {
      message: 'Forbidden',
      color: '#DD4477'
    },
    404: {
      message: 'Not Found',
      color: '#FF9900'
    },
    405: {
      message: 'Method Not Allowed',
      color: '#990099'
    },
    409: {
      message: 'Conflict',
      color: '#AAAA11'
    },
    500: {
      message: 'Internal Server Error',
      color: '#DC3912'
    },
    501: {
      message: 'Not Implemented',
      color: '#6633CC'
    },
    502: {
      message: 'Bad Gateway',
      color: '#0099C6'
    },
    503: {
      message: 'Service Unavailable',
      color: '#C0C0C0'
    }
  }
};