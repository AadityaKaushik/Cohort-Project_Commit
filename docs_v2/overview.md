# Cohort: Project Overview

Cohort is a web app for students who are applying to jobs and internships. It keeps track of every application and helps you prepare for interviews using what actually happened in your past ones.

## The problem

When you apply to 40+ places in a season, a spreadsheet stops working pretty quickly. We kept seeing the same three things go wrong:

- Applications go quiet and nobody notices until it is too late to follow up.
- What you were asked in an interview is mostly forgotten in a day or two.
- Preparation follows a generic question list instead of the topics you are actually weak in.

## What Cohort does

- **Tracking.** Applications go through stages (Saved, Applied, OA, Interview, Final, Offer), shown as a list or a drag-and-drop board. Every status change is saved, and applications that have been quiet longer than your usual wait get flagged. There are also contacts, company pages, offer comparison and weekly goals.
- **Interviews.** Each round is logged with the questions asked. A short debrief after the round (under 3 minutes) records how it went, and any question can be saved to your question bank.
- **Preparation.** A question bank of 288 questions across 48 topics. Practice is logged, and each topic has a mastery score that drops if you stop practising. There is a daily review queue, a timed mock interview, a STAR story bank and a readiness score for each job.
- **Insights.** Funnel, conversion rates, rejection stages and time per stage. Every number shows how many applications it is based on. Recommendations say why they were made and how strong the evidence is.
- **Accounts.** Email and password with a 6-digit email code, or Google, GitHub or LinkedIn sign-in. You can export or delete all your data.

## Federated learning

Cohort has a shared model that learns from everyone's preparation patterns without collecting anyone's data. Each opted-in user trains the model on their own data, and only a clipped, noised update is sent. The server averages these updates, and refuses any round with fewer than 3 people. A Privacy Ledger shows each user exactly what was sent from their account. Taking part is off by default.

## Team

IT314 Software Engineering project.

| Department | Members |
|---|---|
| Backend | Anuj Madhani, Harshit Goyal, Achintya Chandwani, Dev Bhavsar |
| Frontend | Anshul Rai, Chinmay Dak |
| AI/ML | Aaditya Kaushik, Nirmal Nakum, Ved Dhanani, Abhishek Singh |
