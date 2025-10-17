#!/usr/bin/env python3
"""
Bulk Email Sender using Microsoft Graph API
Sends personalized emails to contacts from a JSON file.
"""

import json
import time
import os
import sys
import base64
from typing import List, Dict, Optional
from datetime import datetime
from pathlib import Path

import requests
from msal import ConfidentialClientApplication
from dotenv import load_dotenv


class EmailSender:
    """Handles authentication and email sending via Microsoft Graph API."""
    
    def __init__(self):
        """Initialize the email sender with configuration from .env file."""
        load_dotenv()
        
        self.client_id = os.getenv('CLIENT_ID')
        self.client_secret = os.getenv('CLIENT_SECRET')
        self.tenant_id = os.getenv('TENANT_ID')
        self.sender_email = os.getenv('SENDER_EMAIL')
        self.from_email = os.getenv('FROM_EMAIL', self.sender_email)  # Use alias if provided
        
        if not all([self.client_id, self.client_secret, self.tenant_id, self.sender_email]):
            raise ValueError("Missing required environment variables. Check your .env file.")
        
        self.authority = f"https://login.microsoftonline.com/{self.tenant_id}"
        self.scopes = ["https://graph.microsoft.com/.default"]
        self.graph_endpoint = "https://graph.microsoft.com/v1.0"
        
        self.app = ConfidentialClientApplication(
            self.client_id,
            authority=self.authority,
            client_credential=self.client_secret
        )
        
        self.access_token = None
        self.sent_count = 0
        self.failed_count = 0
        self.failed_emails = []
        
    def authenticate(self) -> bool:
        """Authenticate with Microsoft Graph API and get access token."""
        try:
            result = self.app.acquire_token_silent(self.scopes, account=None)
            
            if not result:
                result = self.app.acquire_token_for_client(scopes=self.scopes)
            
            if "access_token" in result:
                self.access_token = result["access_token"]
                print("✓ Successfully authenticated with Microsoft Graph API")
                return True
            else:
                error = result.get("error_description", result.get("error", "Unknown error"))
                print(f"✗ Authentication failed: {error}")
                return False
                
        except Exception as e:
            print(f"✗ Authentication error: {str(e)}")
            return False
    
    def load_email_template(self, template_path: str = "email_template.html") -> str:
        """Load the email template from file."""
        try:
            with open(template_path, 'r', encoding='utf-8') as f:
                return f.read()
        except FileNotFoundError:
            print(f"✗ Template file not found: {template_path}")
            sys.exit(1)
    
    def generate_dashboard_link(self, region: int, reg_number: int) -> str:
        """
        Generate a personalized dashboard link based on region and candidate number.
        Format follows the dashboardContent.ts encoding:
        - r:0 (Estonia-wide)
        - r:{region} (their region)
        - c:{region}-{regNumber} (their candidate with region prefix)
        """
        # Create the shorthand format: r:0,r:region,c:region-regNumber
        shorthand = f"r:0,r:{region},c:{region}-{reg_number}"
        # Base64 encode
        encoded = base64.b64encode(shorthand.encode('utf-8')).decode('utf-8')
        # Create the full URL
        return f"https://valimisohtu.ee?d={encoded}"
    
    def personalize_email(self, template: str, contact: Dict) -> str:
        """Personalize the email template with contact information."""
        # Format names: capitalize first letter of each word, lowercase the rest
        forename = contact.get('forename', '').title()
        surename = contact.get('surename', '').title()
        region = contact.get('region', 0)
        reg_number = contact.get('regNumber', 0)
        
        # Generate personalized dashboard link
        personalized_link = self.generate_dashboard_link(region, reg_number)
        
        result = template
        result = result.replace('{forename}', forename)
        result = result.replace('{surename}', surename)
        result = result.replace('{region}', str(region))
        result = result.replace('{regNumber}', str(reg_number))
        result = result.replace('{email}', contact.get('email', ''))
        result = result.replace('{link}', personalized_link)
        return result
    
    def send_email(
        self, 
        to_email: str, 
        subject: str, 
        body_html: str,
        dry_run: bool = False
    ) -> bool:
        """
        Send an email via Microsoft Graph API.
        
        Args:
            to_email: Recipient email address
            subject: Email subject
            body_html: Email body in HTML format
            dry_run: If True, don't actually send the email
            
        Returns:
            True if email was sent successfully, False otherwise
        """
        if dry_run:
            print(f"  [DRY RUN] Would send to: {to_email}")
            return True
        
        endpoint = f"{self.graph_endpoint}/users/{self.sender_email}/sendMail"
        
        email_msg = {
            "message": {
                "subject": subject,
                "body": {
                    "contentType": "HTML",
                    "content": body_html
                },
                "from": {
                    "emailAddress": {
                        "address": self.from_email,
                        "name": "KOV2025 Valimised"
                    }
                },
                "toRecipients": [
                    {
                        "emailAddress": {
                            "address": to_email
                        }
                    }
                ]
            },
            "saveToSentItems": "true"
        }
        
        headers = {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json"
        }
        
        try:
            response = requests.post(endpoint, headers=headers, json=email_msg)
            
            if response.status_code == 202:
                return True
            else:
                print(f"  ✗ Failed: {response.status_code} - {response.text}")
                return False
                
        except Exception as e:
            print(f"  ✗ Error: {str(e)}")
            return False
    
    def load_contacts(self, contacts_path: str = "contacts-filtered.json") -> List[Dict]:
        """Load contacts from JSON file."""
        try:
            with open(contacts_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            print(f"✗ Contacts file not found: {contacts_path}")
            sys.exit(1)
        except json.JSONDecodeError as e:
            print(f"✗ Error parsing contacts file: {str(e)}")
            sys.exit(1)
    
    def send_bulk_emails(
        self,
        contacts: List[Dict],
        subject: str,
        template: str,
        delay: float = 1.0,
        dry_run: bool = False,
        start_index: int = 0,
        max_emails: Optional[int] = None
    ):
        """
        Send emails to all contacts with rate limiting and error handling.
        
        Args:
            contacts: List of contact dictionaries
            subject: Email subject line
            template: Email template HTML
            delay: Delay in seconds between emails (default: 1.0)
            dry_run: If True, don't actually send emails
            start_index: Start from this contact index (for resuming)
            max_emails: Maximum number of emails to send (None = all)
        """
        total_contacts = len(contacts)
        end_index = min(start_index + max_emails, total_contacts) if max_emails else total_contacts
        
        # Create failed emails file path (write incrementally)
        failed_path = f"failed_emails_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        print(f"\n{'='*60}")
        print(f"Starting email campaign")
        print(f"{'='*60}")
        print(f"Total contacts: {total_contacts}")
        print(f"Sending to: {start_index + 1} to {end_index}")
        print(f"Delay between emails: {delay}s")
        print(f"Mode: {'DRY RUN' if dry_run else 'LIVE'}")
        if not dry_run:
            print(f"Failed emails log: {failed_path}")
        print(f"{'='*60}\n")
        
        start_time = datetime.now()
        
        for i in range(start_index, end_index):
            contact = contacts[i]
            email = contact.get('email', '').strip()
            
            if not email:
                print(f"[{i+1}/{total_contacts}] Skipping - no email address")
                continue
            
            forename = contact.get('forename', '')
            surename = contact.get('surename', '')
            
            print(f"[{i+1}/{total_contacts}] Sending to: {forename} {surename} <{email}>")
            
            # Personalize the email
            personalized_body = self.personalize_email(template, contact)
            
            # Send the email
            success = self.send_email(email, subject, personalized_body, dry_run)
            
            if success:
                self.sent_count += 1
                print(f"  ✓ Sent successfully")
            else:
                self.failed_count += 1
                # Store the complete contact entry for retry later
                failed_entry = contact.copy()
                failed_entry['_original_index'] = i  # Keep track of original position
                self.failed_emails.append(failed_entry)
                
                # Write failed email to file immediately (in case of Ctrl+C)
                if not dry_run:
                    try:
                        with open(failed_path, 'w', encoding='utf-8') as f:
                            json.dump(self.failed_emails, f, indent=2, ensure_ascii=False)
                    except Exception as e:
                        print(f"  ⚠️  Warning: Could not write to failed emails file: {e}")
            
            # Rate limiting
            if i < end_index - 1:  # Don't sleep after the last email
                time.sleep(delay)
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        # Print summary
        print(f"\n{'='*60}")
        print(f"Campaign Summary")
        print(f"{'='*60}")
        print(f"Total processed: {end_index - start_index}")
        print(f"Successfully sent: {self.sent_count}")
        print(f"Failed: {self.failed_count}")
        print(f"Duration: {duration:.2f} seconds")
        print(f"{'='*60}\n")
        
        if self.failed_emails:
            print("Failed emails:")
            for failed in self.failed_emails:
                forename = failed.get('forename', '')
                surename = failed.get('surename', '')
                email = failed.get('email', '')
                original_index = failed.get('_original_index', '?')
                print(f"  - [{original_index}] {forename} {surename} <{email}>")
            
            # File was already written incrementally, just remind user
            print(f"\n✓ Failed emails saved to: {failed_path}")
            print(f"You can retry with: python send_emails.py --contacts {failed_path}")


def main():
    """Main function to run the email campaign."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Send bulk emails via Microsoft Graph API')
    parser.add_argument('--subject', type=str, default='KOV2025 valimistulemuste avalikustamine',
                       help='Email subject line')
    parser.add_argument('--template', type=str, default='email_template.html',
                       help='Path to email template file')
    parser.add_argument('--contacts', type=str, default='contacts-filtered.json',
                       help='Path to contacts JSON file')
    parser.add_argument('--delay', type=float, default=1.0,
                       help='Delay between emails in seconds (default: 1.0)')
    parser.add_argument('--dry-run', action='store_true',
                       help='Test run without actually sending emails')
    parser.add_argument('--start', type=int, default=0,
                       help='Start from this contact index (for resuming)')
    parser.add_argument('--max', type=int, default=None,
                       help='Maximum number of emails to send')
    
    args = parser.parse_args()
    
    # Initialize email sender
    sender = EmailSender()
    
    # Authenticate
    if not sender.authenticate():
        print("Authentication failed. Please check your credentials in .env file.")
        sys.exit(1)
    
    # Load contacts and template
    contacts = sender.load_contacts(args.contacts)
    template = sender.load_email_template(args.template)
    
    # Confirmation prompt
    if not args.dry_run:
        total_to_send = min(args.max, len(contacts) - args.start) if args.max else len(contacts) - args.start
        print(f"\n⚠️  WARNING: You are about to send {total_to_send} REAL emails!")
        response = input("Type 'yes' to continue: ")
        if response.lower() != 'yes':
            print("Aborted.")
            sys.exit(0)
    
    # Send emails
    try:
        sender.send_bulk_emails(
            contacts=contacts,
            subject=args.subject,
            template=template,
            delay=args.delay,
            dry_run=args.dry_run,
            start_index=args.start,
            max_emails=args.max
        )
    except KeyboardInterrupt:
        print("\n\n⚠️  Interrupted by user (Ctrl+C)")
        print(f"\n{'='*60}")
        print(f"Campaign interrupted - Summary:")
        print(f"{'='*60}")
        print(f"Successfully sent: {sender.sent_count}")
        print(f"Failed: {sender.failed_count}")
        if sender.failed_emails and not args.dry_run:
            print(f"\n✓ Failed emails were saved incrementally during the run.")
            print(f"Check the most recent failed_emails_*.json file in this directory.")
        print(f"{'='*60}")
        sys.exit(130)  # Standard exit code for Ctrl+C


if __name__ == "__main__":
    main()

