import React from 'react'
import CouchbaseLite from './react-native-cbl'
import PropTypes from 'prop-types'
import fastDeepEqual from 'fast-deep-equal'

export class CBLConnection {
  constructor(config) {
    this.config = config
  }

  connect() {
    if (!this.promise) {
      this.promise = CouchbaseLite.openDb(this.config.dbName, false).then( () =>
          CouchbaseLite.getDocument('_design/main')
      ).then( ddoc => {
        if (!fastDeepEqual(ddoc.views, this.config.views)) {
          return CouchbaseLite.updateDocument( '_design/main', { views: this.config.views } )
        } else {
          return Promise.resolve()
        }
      }).then( () => {
        if (this.config.syncGatewayUrl) {
          return CouchbaseLite.startReplication( this.config.syncGatewayUrl, null )
        } else {
          return Promise.resolve()
        }
      })
    }
    return this.promise
  }
}

export class CBLConnector extends React.Component {
  static childContextTypes = {
    cblConnection: PropTypes.object,
  }

  constructor(props, ...args) {
    super(props, ...args)

    props.connection.connect()
  }

  getChildContext() {
    return {
      cblConnection: this.props.connection,
    }
  }

  render() {
    return this.props.children
  }
}
