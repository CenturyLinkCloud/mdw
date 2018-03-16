---
title: Introduction
permalink: /docs/intro/
overview: true
---

MDW stands for Model Driven Workflow.  It's a cloud-enabled, Java-based service framework with the aim of delivering 
workflow-style benefits to all types of applications.  MDW specializes in microservice orchestration.  It comes with all
the tools to model your app flow, and to visually make sense of your microservices.  Under the hood is a robust, scalable,
extensible runtime engine.  MDW also comes with a toolkit for monitoring and analysis to facilitate continuous improvement.

The central idea of MDW is to foster collaboration by elevating logic out of source code and into a graphical, flowchart-style 
representation that can be understood and owned by the rightful domain experts. Here's what this looks like for the simple
bug-handling workflow in the [mdw-demo](https://github.com/CenturyLinkCloud/mdw-demo) example project:

![create bug](../../img/create_bug.png)

This flow is invoked through REST with a JSON payload which by convention is attached as a runtime value with the name `request`.
Through the happy path a bug object is saved, a subflow is launched asynchronously, and an HTTP 201 (Created) response is returned
to the caller. 

Ready to find out more?  Click Next to continue to the high-level [Overview](../overview/), 
or dive straight in to the [Cookbook](../guides/mdw-cookbook) to walk through how this workflow was built.